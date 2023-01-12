/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LuaC;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LuaCoverage {
    private static final Logger LOG = LoggerFactory.getLogger(LuaCoverage.class);
    private static final Path ROOT = new File("src/main/resources/data/computercraft/lua").toPath();
    private static final Path BIOS = ROOT.resolve("bios.lua");
    private static final Path APIS = ROOT.resolve("rom/apis");
    private static final Path STARTUP = ROOT.resolve("rom/startup.lua");
    private static final Path SHELL = ROOT.resolve("rom/programs/shell.lua");
    private static final Path MULTISHELL = ROOT.resolve("rom/programs/advanced/multishell.lua");
    private static final Path TREASURE = ROOT.resolve("treasure");

    private final Map<String, Int2IntMap> coverage;
    private final String blank;
    private final String zero;
    private final String countFormat;

    LuaCoverage(Map<String, Int2IntMap> coverage) {
        this.coverage = coverage;

        var max = coverage.values().stream()
            .flatMapToInt(x -> x.values().intStream())
            .max().orElse(0);
        var maxLen = Math.max(1, (int) Math.ceil(Math.log10(max)));
        blank = " ".repeat(maxLen + 1);
        zero = "*".repeat(maxLen) + "0";
        countFormat = "%" + (maxLen + 1) + "d";
    }

    void write(Writer out) throws IOException {
        Files.find(ROOT, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile() && !path.startsWith(TREASURE)).forEach(path -> {
            var relative = ROOT.relativize(path);
            var full = relative.toString().replace('\\', '/');
            if (!full.endsWith(".lua")) return;

            var possiblePaths = Stream.of(
                coverage.remove("/" + full),
                path.equals(BIOS) ? coverage.remove("bios.lua") : null,
                path.equals(STARTUP) ? coverage.remove("startup.lua") : null,
                path.equals(SHELL) ? coverage.remove("shell.lua") : null,
                path.equals(MULTISHELL) ? coverage.remove("multishell.lua") : null,
                path.startsWith(APIS) ? coverage.remove(path.getFileName().toString()) : null
            );
            var files = possiblePaths
                .filter(Objects::nonNull)
                .flatMap(x -> x.int2IntEntrySet().stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));

            try {
                writeCoverageFor(out, path, files);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        for (var filename : coverage.keySet()) {
            if (filename.startsWith("/test-rom/")) continue;
            LOG.warn("Unknown file {}", filename);
        }
    }

    private void writeCoverageFor(Writer out, Path fullName, Map<Integer, Integer> visitedLines) throws IOException {
        if (!Files.exists(fullName)) {
            LOG.error("Cannot locate file {}", fullName);
            return;
        }

        var activeLines = getActiveLines(fullName.toFile());

        out.write("==============================================================================\n");
        out.write(fullName.toString().replace('\\', '/'));
        out.write("\n");
        out.write("==============================================================================\n");

        try (var reader = Files.newBufferedReader(fullName)) {
            String line;
            var lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                var count = visitedLines.get(lineNo);
                if (count != null) {
                    out.write(String.format(countFormat, count));
                } else if (activeLines.contains(lineNo)) {
                    out.write(zero);
                } else {
                    out.write(blank);
                }

                out.write(' ');
                out.write(line);
                out.write("\n");
            }
        }
    }

    private static IntSet getActiveLines(File file) throws IOException {
        IntSet activeLines = new IntOpenHashSet();
        try (InputStream stream = new FileInputStream(file)) {
            var proto = LuaC.compile(stream, "@" + file.getPath());
            Queue<Prototype> queue = new ArrayDeque<>();
            queue.add(proto);

            while ((proto = queue.poll()) != null) {
                var lines = proto.lineinfo;
                if (lines != null) {
                    for (var line : lines) {
                        activeLines.add(line);
                    }
                }
                if (proto.p != null) Collections.addAll(queue, proto.p);
            }
        } catch (CompileException e) {
            throw new IllegalStateException("Cannot compile", e);
        }

        return activeLines;
    }
}
