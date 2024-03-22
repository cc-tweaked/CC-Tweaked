// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LuaC;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;

class LuaCoverage {
    private static final Logger LOG = LoggerFactory.getLogger(LuaCoverage.class);
    private static final Path ROOT = new File("src/main/resources/data/computercraft/lua").toPath();

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
        try (var files = Files.find(ROOT, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile())) {
            files.forEach(path -> writeSingleFile(out, path));
        }

        for (var filename : coverage.keySet()) {
            if (filename.startsWith("/test-rom/")) continue;
            LOG.warn("Unknown file {}", filename);
        }
    }

    private void writeSingleFile(Writer out, Path path) {
        var relative = ROOT.relativize(path);
        var full = relative.toString().replace('\\', '/');
        if (!full.endsWith(".lua")) return;

        var possiblePaths = coverage.remove("/" + full);
        if (possiblePaths == null) possiblePaths = coverage.remove(full);
        if (possiblePaths == null) {
            possiblePaths = Int2IntMaps.EMPTY_MAP;
            LOG.warn("{} has no coverage data", full);
        }

        try {
            writeCoverageFor(out, path, possiblePaths);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
        Queue<Prototype> queue = new ArrayDeque<>();

        try (InputStream stream = new FileInputStream(file)) {
            var proto = LuaC.compile(new LuaState(), stream, "@" + file.getPath());
            queue.add(proto);
        } catch (LuaError | CompileException e) {
            throw new IllegalStateException("Cannot compile", e);
        }

        Prototype proto;
        while ((proto = queue.poll()) != null) {
            var lines = proto.lineInfo;
            if (lines != null) {
                for (var line : lines) {
                    activeLines.add(line);
                }
            }
            if (proto.children != null) Collections.addAll(queue, proto.children);
        }

        return activeLines;
    }
}
