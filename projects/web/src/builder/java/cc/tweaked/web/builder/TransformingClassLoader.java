// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.builder;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * A class loader which can {@linkplain #addTransformer(BiFunction) transform} and {@linkplain #remapClass(String, String)
 * remap/rename} classes.
 * <p>
 * When loading classes, this behaves much like {@link java.net.URLClassLoader}, loading files from a list of paths.
 * However, when the TeaVM compiler requests the class bytes for compilation, we run the class through a list of
 * transformers first, patching the classes to function in a Javascript environment.
 */
public class TransformingClassLoader extends ClassLoader {
    private final Map<String, String> remappedClasses = new HashMap<>();
    private final Map<String, Path> remappedResources = new HashMap<>();
    private final List<BiFunction<String, ClassVisitor, ClassVisitor>> transformers = new ArrayList<>();

    private final Remapper remapper = new Remapper() {
        @Override
        public String map(String internalName) {
            return remappedClasses.getOrDefault(internalName, internalName);
        }
    };

    private final List<Path> classpath;

    // Cache of the last transformed file - TeaVM tends to call getResourceAsStream multiple times.
    private @Nullable TransformedClass lastFile;

    public TransformingClassLoader(List<Path> classpath) {
        this.classpath = classpath;

        addTransformer((name, cv) -> new ClassRemapper(cv, remapper));
    }

    public void addTransformer(BiFunction<String, ClassVisitor, ClassVisitor> transform) {
        transformers.add(transform);
    }

    public void remapClass(String from, String to, Path fromLocation) {
        remappedClasses.put(from, to);
        remappedResources.put(to + ".class", fromLocation);
    }

    public void remapClass(String from, String to) {
        remappedClasses.put(from, to);
    }

    private @Nullable Path findUnmappedFile(String name) {
        // For some odd reason, we try to resolve the generated lambda classes. This includes classes called
        // things like <linit>lambda, which is an invalid path on Windows. Detect those, and abort early.
        if (name.indexOf("<") >= 0) return null;

        return classpath.stream().map(x -> x.resolve(name)).filter(Files::exists).findFirst().orElse(null);
    }

    private @Nullable Path findFile(String name) {
        var path = remappedResources.get(name);
        return path != null ? path : findUnmappedFile(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // findClass is only called at compile time, and so we load the original class, not the remapped one.
        // Yes, this is super cursed.
        var path = findUnmappedFile(name.replace('.', '/') + ".class");
        if (path == null) throw new ClassNotFoundException();

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed reading " + name, e);
        }

        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    public @Nullable InputStream getResourceAsStream(String name) {
        if (!name.endsWith(".class") || name.startsWith("java/") || name.startsWith("javax/")) {
            return super.getResourceAsStream(name);
        }

        var lastFile = this.lastFile;
        if (lastFile != null && lastFile.name().equals(name)) return new ByteArrayInputStream(lastFile.contents());

        var path = findFile(name);
        if (path == null) {
            System.out.printf("Cannot find %s. Falling back to system class loader.\n", name);
            return super.getResourceAsStream(name);
        }

        ClassReader reader;
        try (var stream = Files.newInputStream(path)) {
            reader = new ClassReader(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + name, e);
        }

        var writer = new ClassWriter(reader, 0);
        var className = reader.getClassName();
        ClassVisitor sink = writer;
        for (var transformer : transformers) sink = transformer.apply(className, sink);
        reader.accept(sink, 0);

        var bytes = writer.toByteArray();
        this.lastFile = new TransformedClass(name, bytes);
        return new ByteArrayInputStream(bytes);
    }

    @Override
    protected @Nullable URL findResource(String name) {
        var path = findFile(name);
        return path == null ? null : toURL(path);
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        var path = remappedResources.get(name);
        return new IteratorEnumeration<>(
            (path == null ? classpath.stream().map(x -> x.resolve(name)) : Stream.of(path))
                .filter(Files::exists)
                .map(TransformingClassLoader::toURL)
                .iterator()
        );
    }

    @SuppressWarnings("JdkObsolete")
    private record IteratorEnumeration<T>(Iterator<T> iterator) implements Enumeration<T> {
        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public T nextElement() {
            return iterator.next();
        }
    }

    private static URL toURL(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Cannot convert " + path + " to a URL", e);
        }
    }

    private record TransformedClass(String name, byte[] contents) {
    }
}
