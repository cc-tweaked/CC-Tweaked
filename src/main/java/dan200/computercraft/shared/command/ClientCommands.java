/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command;

import dan200.computercraft.shared.util.IDAssigner;
import net.minecraft.Util;

import java.io.File;

/**
 * Basic client-side commands.
 *
 * Simply hooks into client chat messages and intercepts matching strings.
 */
public final class ClientCommands
{
    public static final String OPEN_COMPUTER = "/computercraft open-computer ";

    private ClientCommands()
    {
    }

    public static boolean onClientSendMessage( String message )
    {
        // Emulate the command on the client side
        if( message.startsWith( OPEN_COMPUTER ) )
        {
            String idStr = message.substring( OPEN_COMPUTER.length() ).trim();
            int id;
            try
            {
                id = Integer.parseInt( idStr );
            }
            catch( NumberFormatException ignore )
            {
                return true;
            }

            File file = new File( IDAssigner.getDir(), "computer/" + id );
            if( !file.isDirectory() ) return true;

            Util.getPlatform().openFile( file );
            return true;
        }
        return false;
    }
}
