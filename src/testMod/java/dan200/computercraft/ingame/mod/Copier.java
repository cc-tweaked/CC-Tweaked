/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import com.google.common.io.MoreFiles;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

final class Copier extends SimpleFileVisitor<Path>
{
    private final Path sourceDir;
    private final Path targetDir;
    private final Predicate<Path> predicate;

    private Copier( Path sourceDir, Path targetDir, Predicate<Path> predicate )
    {
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.predicate = predicate;
    }

    @Override
    public FileVisitResult visitFile( Path file, BasicFileAttributes attributes ) throws IOException
    {
        if( predicate.test( file ) ) Files.copy( file, targetDir.resolve( sourceDir.relativize( file ) ) );
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attributes ) throws IOException
    {
        Path newDir = targetDir.resolve( sourceDir.relativize( dir ) );
        Files.createDirectories( newDir );
        return FileVisitResult.CONTINUE;
    }

    public static void copy( Path from, Path to ) throws IOException
    {
        Files.walkFileTree( from, new Copier( from, to, p -> true ) );
    }

    public static void replicate( Path from, Path to ) throws IOException
    {
        replicate( from, to, p -> true );
    }

    public static void replicate( Path from, Path to, Predicate<Path> check ) throws IOException
    {
        if( Files.exists( to ) ) MoreFiles.deleteRecursively( to );
        Files.walkFileTree( from, new Copier( from, to, check ) );
    }
}
