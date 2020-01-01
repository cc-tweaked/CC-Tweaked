/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class IDAssigner
{
    private IDAssigner()
    {
    }

    public static int getNextIDFromDirectory( String path )
    {
        return getNextIDFromDirectory( new File( ComputerCraft.getWorldDir(), path ) );
    }

    public static int getNextIDFromFile( String path )
    {
        return getNextIDFromFile( new File( ComputerCraft.getWorldDir(), path ) );
    }

    public static int getNextIDFromDirectory( File dir )
    {
        return getNextID( dir, true );
    }

    public static int getNextIDFromFile( File file )
    {
        return getNextID( file, false );
    }

    private static int getNextID( File location, boolean directory )
    {
        // Determine where to locate ID file
        File lastIdFile;
        if( directory )
        {
            location.mkdirs();
            lastIdFile = new File( location, "lastid.txt" );
        }
        else
        {
            location.getParentFile().mkdirs();
            lastIdFile = location;
        }

        // Try to determine the id
        int id = 0;
        if( !lastIdFile.exists() )
        {
            // If an ID file doesn't exist, determine it from the file structure
            if( directory && location.exists() && location.isDirectory() )
            {
                String[] contents = location.list();
                for( String content : contents )
                {
                    try
                    {
                        id = Math.max( Integer.parseInt( content ) + 1, id );
                    }
                    catch( NumberFormatException ignored )
                    {
                        // Skip files which aren't numbers
                    }
                }
            }
        }
        else
        {
            // If an ID file does exist, parse the file to get the ID string
            String idString;
            try
            {
                FileInputStream in = new FileInputStream( lastIdFile );
                InputStreamReader isr = new InputStreamReader( in, StandardCharsets.UTF_8 );
                try( BufferedReader br = new BufferedReader( isr ) )
                {
                    idString = br.readLine();
                }
            }
            catch( IOException e )
            {
                ComputerCraft.log.error( "Cannot open ID file '" + lastIdFile + "'", e );
                return 0;
            }

            try
            {
                id = Integer.parseInt( idString ) + 1;
            }
            catch( NumberFormatException e )
            {
                ComputerCraft.log.error( "Cannot parse ID file '" + lastIdFile + "', perhaps it is corrupt?", e );
                return 0;
            }
        }

        // Write the lastID file out with the new value
        try
        {
            try( BufferedWriter out = new BufferedWriter( new FileWriter( lastIdFile, false ) ) )
            {
                out.write( Integer.toString( id ) );
                out.newLine();
            }
        }
        catch( IOException e )
        {
            ComputerCraft.log.error( "An error occurred while trying to create the computer folder. Please check you have relevant permissions.", e );
        }

        return id;
    }
}
