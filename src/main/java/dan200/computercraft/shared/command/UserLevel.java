/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/**
 * The level a user must be at in order to execute a command.
 */
public enum UserLevel implements Predicate<CommandSourceStack>
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
    public boolean test( CommandSourceStack source )
    {
        if( this == ANYONE ) return true;
        if( this == OWNER ) return isOwner( source );
        if( this == OWNER_OP && isOwner( source ) ) return true;
        return source.hasPermission( toLevel() );
    }

    /**
     * Take the union of two {@link UserLevel}s.
     * <p>
     * This satisfies the property that for all sources {@code s}, {@code a.test(s) || b.test(s) == (a ∪ b).test(s)}.
     *
     * @param left  The first user level to take the union of.
     * @param right The second user level to take the union of.
     * @return The union of two levels.
     */
    public static UserLevel union( UserLevel left, UserLevel right )
    {
        if( left == right ) return left;

        // x ∪ ANYONE = ANYONE
        if( left == ANYONE || right == ANYONE ) return ANYONE;

        // x ∪ OWNER = OWNER
        if( left == OWNER ) return right;
        if( right == OWNER ) return left;

        // At this point, we have x != y and x, y ∈ { OP, OWNER_OP }.
        return OWNER_OP;
    }

    private static boolean isOwner( CommandSourceStack source )
    {
        MinecraftServer server = source.getServer();
        Entity sender = source.getEntity();
        return server.isDedicatedServer()
            ? source.getEntity() == null && source.hasPermission( 4 ) && source.getTextName().equals( "Server" )
            : sender instanceof Player player && server.isSingleplayerOwner( player.getGameProfile() );
    }
}
