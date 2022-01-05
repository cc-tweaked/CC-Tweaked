/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

public class FileSystemException extends Exception
{
    private static final long serialVersionUID = -2500631644868104029L;

    FileSystemException( String s )
    {
        super( s );
    }
}
