// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

final class Copier extends SimpleFileVisitor<Path> {
    private final Path sourceDir;
    private final Path targetDir;
    private final Predicate<Path> predicate;

    private Copier(Path sourceDir, Path targetDir, Predicate<Path> predicate) {
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.predicate = predicate;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
        if (predicate.test(file)) Files.copy(file, targetDir.resolve(sourceDir.relativize(file)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
        var newDir = targetDir.resolve(sourceDir.relativize(dir));
        Files.createDirectories(newDir);
        return FileVisitResult.CONTINUE;
    }

    public static void copy(Path from, Path to) throws IOException {
        Files.walkFileTree(from, new Copier(from, to, p -> true));
    }

    public static void replicate(Path from, Path to) throws IOException {
        replicate(from, to, p -> true);
    }

    public static void replicate(Path from, Path to, Predicate<Path> check) throws IOException {
        if (Files.exists(to)) MoreFiles.deleteRecursively(to, RecursiveDeleteOption.ALLOW_INSECURE);
        Files.walkFileTree(from, new Copier(from, to, check));
    }
}
