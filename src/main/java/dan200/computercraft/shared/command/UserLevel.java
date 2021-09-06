/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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
        if( this == OWNER ) return isOwner( source );
        if( this == OWNER_OP && isOwner( source ) ) return true;
        return source.hasPermission( toLevel() );
    }

    private static boolean isOwner( CommandSource source )
    {
        MinecraftServer server = source.getServer();
        Entity sender = source.getEntity();
        return server.isDedicatedServer()
            ? source.getEntity() == null && source.hasPermission( 4 ) && source.getTextName().equals( "Server" )
            : sender instanceof PlayerEntity && ((PlayerEntity) sender).getGameProfile().getName().equalsIgnoreCase( server.getServerModName() );
    }
}
