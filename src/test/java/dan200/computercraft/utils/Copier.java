package dan200.computercraft.utils;

import com.google.common.io.MoreFiles;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Copier extends SimpleFileVisitor<Path>
{
    private final Path sourceDir;
    private final Path targetDir;

    private Copier( Path sourceDir, Path targetDir )
    {
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
    }

    @Override
    public FileVisitResult visitFile( Path file, BasicFileAttributes attributes ) throws IOException
    {
        Path targetFile = targetDir.resolve( sourceDir.relativize( file ) );
        Files.copy( file, targetFile );
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
        Files.walkFileTree( from, new Copier( from, to ) );
    }

    public static void replicate( Path from, Path to ) throws IOException
    {
        if( Files.exists( to ) ) MoreFiles.deleteRecursively( to );
        copy( from, to );
    }
}
