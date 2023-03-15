// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import com.google.auto.service.AutoService;
import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.game.minecraft.Slf4jLogHandler;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.FabricMixinBootstrap;
import net.fabricmc.loader.impl.launch.knot.MixinServiceKnot;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.log.Log;
import org.junit.jupiter.api.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Loads Fabric mods as part of this test run.
 * <p>
 * This sets up a minimalistic {@link FabricLauncherBase}, uses that to load mods, and then acquires an
 * {@link Instrumentation} instance, registering a {@link ClassFileTransformer} to apply mixins and access wideners.
 *
 * @see net.fabricmc.loader.impl.launch.knot.Knot
 */
@AutoService(Extension.class)
public class FabricBootstrap implements Extension {
    private static final Logger LOG = LoggerFactory.getLogger(FabricBootstrap.class);

    public FabricBootstrap() throws ReflectiveOperationException, IOException {
        Log.init(new Slf4jLogHandler());

        readProperties();
        {
            var method = FabricLauncherBase.class.getDeclaredMethod("setProperties", Map.class);
            method.setAccessible(true);
            method.invoke(null, new HashMap<>());
        }

        var provider = new MinecraftGameProvider();
        if (!provider.locateGame(new BasicLauncher(), new String[0])) {
            throw new IllegalStateException("Cannot setup game");
        }

        var loader = FabricLoaderImpl.INSTANCE;
        loader.setGameProvider(provider);
        loader.load();
        loader.freeze();
        loader.loadAccessWideners();

        FabricMixinBootstrap.init(EnvType.CLIENT, loader);
        {
            var method = FabricLauncherBase.class.getDeclaredMethod("finishMixinBootstrapping");
            method.setAccessible(true);
            method.invoke(null);
        }

        IMixinTransformer transformer;
        {
            var method = MixinServiceKnot.class.getDeclaredMethod("getTransformer");
            method.setAccessible(true);
            transformer = (IMixinTransformer) method.invoke(null);
        }

        ByteBuddyAgent.install().addTransformer(new ClassTransformer(transformer));
    }

    private static void readProperties() throws IOException {
        try (var reader = Files.newBufferedReader(Path.of(".gradle/loom-cache/launch.cfg"))) {
            var interesting = false;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(" ") || line.startsWith("\t")) {
                    if (!interesting) continue;

                    line = line.strip();
                    var index = line.indexOf('=');

                    if (index >= 0) {
                        System.setProperty(line.substring(0, index), line.substring(index + 1));
                    } else {
                        System.setProperty(line, "");
                    }
                } else {
                    interesting = line.equals("commonProperties") || line.equals("clientProperties");
                }
            }
        }
    }

    private static final class BasicLauncher extends FabricLauncherBase {
        private final List<Path> classpath = new ArrayList<>();

        BasicLauncher() {
            for (var entry : Splitter.on(File.pathSeparatorChar).split(System.getProperty("java.class.path"))) {
                var path = Paths.get(entry);
                if (Files.exists(path)) classpath.add(LoaderUtil.normalizeExistingPath(path));
            }
        }

        @Override
        public void addToClassPath(Path path, String... allowedPrefixes) {
            classpath.add(path);
        }

        @Override
        public void setAllowedPrefixes(Path path, String... prefixes) {
        }

        @Override
        public void setValidParentClassPath(Collection<Path> paths) {
            throw new UnsupportedOperationException("setValidParentClassPath");
        }

        @Override
        public EnvType getEnvironmentType() {
            return EnvType.CLIENT;
        }

        @Override
        public boolean isClassLoaded(String name) {
            return false;
        }

        @Override
        public Class<?> loadIntoTarget(String name) {
            throw new UnsupportedOperationException("loadIntoTarget");
        }

        @Override
        public ClassLoader getTargetClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }

        @Override
        public @Nullable InputStream getResourceAsStream(String name) {
            return BasicLauncher.class.getClassLoader().getResourceAsStream(name);
        }

        @Override
        public @Nullable byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
            try (var stream = BasicLauncher.class.getClassLoader().getResourceAsStream(LoaderUtil.getClassFileName(name))) {
                if (stream == null) return null;
                return ByteStreams.toByteArray(stream);
            }
        }

        @Override
        public Manifest getManifest(Path originPath) {
            throw new UnsupportedOperationException("getManifest");
        }

        @Override
        public boolean isDevelopment() {
            return true;
        }

        @Override
        public String getEntrypoint() {
            throw new UnsupportedOperationException("getEntrypoint");
        }

        @Override
        public String getTargetNamespace() {
            return "named";
        }

        @Override
        public List<Path> getClassPath() {
            return classpath;
        }
    }

    private static final class ClassTransformer implements ClassFileTransformer {
        private final IMixinTransformer transformer;
        private final Set<String> definedClasses = new HashSet<>();
        private final Set<String> generatedClasses;

        private ClassTransformer(IMixinTransformer transformer) {
            this.transformer = transformer;

            try {
                // As we can't hook into classloading itself, we need to track all the classes Mixin has generated. Yes,
                // this is nasty.
                var syntheticRegistryField = transformer.getClass().getDeclaredField("syntheticClassRegistry");
                syntheticRegistryField.setAccessible(true);
                var syntheticRegistry = syntheticRegistryField.get(transformer);

                var classesField = syntheticRegistry.getClass().getDeclaredField("classes");
                classesField.setAccessible(true);
                @SuppressWarnings("unchecked") var classes = (Map<String, ?>) classesField.get(syntheticRegistry);
                generatedClasses = classes.keySet();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized @Nullable byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
            var name = className.replace('/', '.');
            var transformed = FabricTransformer.transform(true, EnvType.CLIENT, name, bytes);
            transformed = transformer.transformClassBytes(name, name, transformed);

            // Keep track of all generated classes that we've seen, and define any new ones. We use ByteBuddy to inject
            // the new class definitions, as doing it ourselves is hard.
            if (generatedClasses.size() > definedClasses.size()) {
                var toDefine = generatedClasses.stream().filter(definedClasses::add).collect(Collectors.toUnmodifiableMap(
                    genName -> genName.replace('/', '.'),
                    genName -> transformer.generateClass(MixinEnvironment.getDefaultEnvironment(), genName)
                ));

                LOG.info("Defining {}", toDefine.keySet());
                try {
                    new ClassInjector.UsingReflection(loader).injectRaw(toDefine);
                } catch (Exception e) {
                    LOG.error("Failed to define {}", className, e);
                }
            }

            return transformed == bytes ? null : transformed;
        }
    }
}
