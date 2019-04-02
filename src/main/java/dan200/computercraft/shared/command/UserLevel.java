/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.function.Predicate;

/**
 * The level a user must be at in order to execute a command.
 */
public enum UserLevel implements Predicate<CommandSource>
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

    @Override
    public boolean test( CommandSource source )
    {
        if( this == ANYONE ) return true;

        // We *always* allow level 0 stuff, even if the
        MinecraftServer server = source.getServer();
        Entity sender = source.getEntity();

        if( server.isSinglePlayer() && sender instanceof EntityPlayer &&
            ((EntityPlayer) sender).getGameProfile().getName().equalsIgnoreCase( server.getServerModName() ) )
        {
            if( this == OWNER || this == OWNER_OP ) return true;
        }

        return source.hasPermissionLevel( toLevel() );
    }
}
