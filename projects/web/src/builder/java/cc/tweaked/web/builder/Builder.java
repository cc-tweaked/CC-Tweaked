// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.builder;

import org.teavm.backend.javascript.JSModuleType;
import org.teavm.common.JsonUtil;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.vm.TeaVMOptimizationLevel;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * The main entrypoint to our Javascript builder.
 * <p>
 * This generates both our classes and resources JS files.
 */
public class Builder {
    public static void main(String[] args) throws Exception {
        try (var scope = new CloseScope()) {
            var input = getPath(scope, "cct.input");
            var classpath = getPath(scope, "cct.classpath");
            var output = getFile("cct.output");
            var version = System.getProperty("cct.version");
            var minify = Boolean.parseBoolean(System.getProperty("cct.minify", "true"));

            buildClasses(input, classpath, output, minify);
            buildResources(version, input, classpath, output);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static void buildClasses(List<Path> input, List<Path> classpath, Path output, boolean minify) throws Exception {
        var remapper = new TransformingClassLoader(classpath);
        // Remap several classes to our stubs. Really we should add all of these to TeaVM, but our current
        // implementations are a bit of a hack.
        remapper.remapClass("java/nio/channels/FileChannel", "cc/tweaked/web/stub/FileChannel");
        remapper.remapClass("java/util/concurrent/locks/ReentrantLock", "cc/tweaked/web/stub/ReentrantLock");
        // Add some additional transformers.
        remapper.addTransformer(PatchCobalt::patch);

        // Scans the main input folders for classes starting with "T", and uses them as an overlay, replacing the
        // original class with this redefinition.
        for (var file : input) {
            traverseClasses(file, (fullName, path) -> {
                var lastPart = fullName.lastIndexOf('/');
                var className = fullName.substring(lastPart + 1);
                if (className.startsWith("T") && Character.isUpperCase(className.charAt(1))) {
                    var originalName = fullName.substring(0, lastPart + 1) + className.substring(1);
                    System.out.printf("Replacing %s with %s\n", originalName, fullName);
                    remapper.remapClass(fullName, originalName, path);
                }
            });
        }

        // Then finally start the compiler!
        var tool = new TeaVMTool();
        tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
        tool.setJsModuleType(JSModuleType.ES2015);
        tool.setTargetDirectory(output.toFile());
        tool.setClassLoader(remapper);
        tool.setMainClass("cc.tweaked.web.Main");

        tool.setOptimizationLevel(TeaVMOptimizationLevel.ADVANCED);
        tool.setObfuscated(minify);

        tool.generate();
        TeaVMProblemRenderer.describeProblems(tool.getDependencyInfo().getCallGraph(), tool.getProblemProvider(), new ConsoleTeaVMToolLog(false));
        if (!tool.getProblemProvider().getSevereProblems().isEmpty()) System.exit(1);
    }

    private static void buildResources(String version, List<Path> input, List<Path> classpath, Path output) throws IOException {
        try (var out = Files.newBufferedWriter(output.resolve("resources.js"))) {
            out.write("export const version = \"");
            JsonUtil.writeEscapedString(out, version);
            out.write("\";\n");
            out.write("export const resources = {\n");

            Stream.of(input, classpath).flatMap(Collection::stream).forEach(root -> {
                var start = root.resolve("data/computercraft/lua");
                if (!Files.exists(start)) return;

                try (var walker = Files.find(start, Integer.MAX_VALUE, (p, a) -> a.isRegularFile())) {
                    walker.forEach(x -> {
                        try {
                            out.write("  \"");
                            JsonUtil.writeEscapedString(out, start.relativize(x).toString());
                            out.write("\": \"");
                            JsonUtil.writeEscapedString(out, Files.readString(x, StandardCharsets.UTF_8));
                            out.write("\",\n");
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            out.write("};\n");
        }
    }

    private static void traverseClasses(Path root, BiConsumer<String, Path> child) throws IOException {
        try (var walker = Files.walk(root)) {
            walker.forEach(entry -> {
                if (Files.isDirectory(entry)) return;

                var name = root.relativize(entry).toString();
                if (!name.endsWith(".class")) return;

                var className = name.substring(0, name.length() - 6).replace(File.separatorChar, '/');

                child.accept(className, entry);
            });
        }
    }

    private static List<Path> getPath(CloseScope scope, String name) {
        return Arrays.stream(System.getProperty(name).split(File.pathSeparator))
            .map(Path::of)
            .filter(Files::exists)
            .map(file -> {
                try {
                    return Files.isDirectory(file) ? file : scope.add(FileSystems.newFileSystem(file)).getPath("/");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .toList();
    }

    private static Path getFile(String name) {
        return Path.of(System.getProperty(name));
    }
}
