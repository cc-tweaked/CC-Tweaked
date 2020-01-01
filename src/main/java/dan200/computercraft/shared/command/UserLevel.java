/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command;

import dan200.computercraft.shared.command.framework.CommandContext;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/**
 * The level a user must be at in order to execute a command.
 */
public enum UserLevel
{
    /**
     * Only can be used by the owner of the server: namely the server console or the player in SSP.
     */
    OWNER,

    /**
     * Can only be used by ops.
     */
    OP,

    /**
     * Can be used by any op, or the player in SSP.
     */
    OWNER_OP,

    /**
     * Can be used by anyone.
     */
    ANYONE;

    public int toLevel()
    {
        switch( this )
        {
            case OWNER:
                return 4;
            case OP:
            case OWNER_OP:
                return 2;
            case ANYONE:
            default:
                return 0;
        }
    }

    public boolean canExecute( CommandContext context )
    {
        if( this == ANYONE ) return true;

        // We *always* allow level 0 stuff, even if the
        MinecraftServer server = context.getServer();
        ICommandSender sender = context.getSender();

        if( server.isSinglePlayer() && sender instanceof EntityPlayerMP &&
            ((EntityPlayerMP) sender).getGameProfile().getName().equalsIgnoreCase( server.getServerOwner() ) )
        {
            if( this == OWNER || this == OWNER_OP ) return true;
        }

        return sender.canUseCommand( toLevel(), context.getRootCommand() );
    }
}
