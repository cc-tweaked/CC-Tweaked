/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;

class LuaCoverage
{
    private static final Logger LOG = LoggerFactory.getLogger( LuaCoverage.class );
    private static final Path ROOT = new File( "src/main/resources/data/computercraft/lua" ).toPath();

    private final Map<String, Int2IntMap> coverage;
    private final String blank;
    private final String zero;
    private final String countFormat;

    LuaCoverage( Map<String, Int2IntMap> coverage )
    {
        this.coverage = coverage;

        int max = coverage.values().stream()
            .flatMapToInt( x -> x.values().stream().mapToInt( y -> y ) )
            .max().orElse( 0 );
        int maxLen = Math.max( 1, (int) Math.ceil( Math.log10( max ) ) );
        blank = Strings.repeat( " ", maxLen + 1 );
        zero = Strings.repeat( "*", maxLen ) + "0";
        countFormat = "%" + (maxLen + 1) + "d";
    }

    void write( Writer out ) throws IOException
    {
        Files.find( ROOT, Integer.MAX_VALUE, ( path, attr ) -> attr.isRegularFile() ).forEach( path -> {
            Path relative = ROOT.relativize( path );
            String full = relative.toString().replace( '\\', '/' );
            if( !full.endsWith( ".lua" ) ) return;

            Int2IntMap possiblePaths = coverage.remove( "/" + full );
            if( possiblePaths == null ) possiblePaths = coverage.remove( full );
            if( possiblePaths == null )
            {
                possiblePaths = Int2IntMaps.EMPTY_MAP;
                LOG.warn( "{} has no coverage data", full );
            }

            try
            {
                writeCoverageFor( out, path, possiblePaths );
            }
            catch( IOException e )
            {
                throw new UncheckedIOException( e );
            }
        } );

        for( String filename : coverage.keySet() )
        {
            if( filename.startsWith( "/test-rom/" ) ) continue;
            LOG.warn( "Unknown file {}", filename );
        }
    }

    private void writeCoverageFor( Writer out, Path fullName, Int2IntMap visitedLines ) throws IOException
    {
        if( !Files.exists( fullName ) )
        {
            LOG.error( "Cannot locate file {}", fullName );
            return;
        }

        IntSet activeLines = getActiveLines( fullName.toFile() );

        out.write( "==============================================================================\n" );
        out.write( fullName.toString().replace( '\\', '/' ) );
        out.write( "\n" );
        out.write( "==============================================================================\n" );

        try( BufferedReader reader = Files.newBufferedReader( fullName ) )
        {
            String line;
            int lineNo = 0;
            while( (line = reader.readLine()) != null )
            {
                lineNo++;
                int count = visitedLines.getOrDefault( lineNo, -1 );
                if( count >= 0 )
                {
                    out.write( String.format( countFormat, count ) );
                }
                else if( activeLines.contains( lineNo ) )
                {
                    out.write( zero );
                }
                else
                {
                    out.write( blank );
                }

                out.write( ' ' );
                out.write( line );
                out.write( "\n" );
            }
        }
    }

    private static IntSet getActiveLines( File file ) throws IOException
    {
        IntSet activeLines = new IntOpenHashSet();
        Queue<Prototype> queue = new ArrayDeque<>();

        try( InputStream stream = Files.newInputStream( file.toPath() ) )
        {
            Prototype proto = LuaC.compile( stream, "@" + file.getPath() );
            queue.add( proto );
        }
        catch( CompileException e )
        {
            throw new IllegalStateException( "Cannot compile", e );
        }

        Prototype proto;
        while( (proto = queue.poll()) != null )
        {
            int[] lines = proto.lineInfo;
            if( lines != null )
            {
                for( int line : lines )
                {
                    activeLines.add( line );
                }
            }
            if( proto.children != null ) Collections.addAll( queue, proto.children );
        }

        return activeLines;
    }
}
