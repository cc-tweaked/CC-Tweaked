/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/**
 * The level a user must be at in order to execute a command.
 */
public enum UserLevel implements Predicate<CommandSourceStack> {
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

    public int toLevel() {
        return switch (this) {
            case OWNER -> 4;
            case OP, OWNER_OP -> 2;
            case ANYONE -> 0;
        };
    }

    @Override
    public boolean test(CommandSourceStack source) {
        if (this == ANYONE) return true;
        if (this == OWNER) return isOwner(source);
        if (this == OWNER_OP && isOwner(source)) return true;
        return source.hasPermission(toLevel());
    }

    private static boolean isOwner(CommandSourceStack source) {
        var server = source.getServer();
        var sender = source.getEntity();
        return server.isDedicatedServer()
            ? source.getEntity() == null && source.hasPermission(4) && source.getTextName().equals("Server")
            : sender instanceof Player player && server.isSingleplayerOwner(player.getGameProfile());
    }
}
