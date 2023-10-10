// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * The level a user must be at in order to execute a command.
 */
public enum UserLevel implements Predicate<CommandSourceStack> {
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
            case OP, OWNER_OP -> 2;
            case ANYONE -> 0;
        };
    }

    @Override
    public boolean test(CommandSourceStack source) {
        if (this == ANYONE) return true;
        if (this == OWNER_OP && isOwner(source)) return true;
        return source.hasPermission(toLevel());
    }

    public boolean test(ServerPlayer source) {
        if (this == ANYONE) return true;
        if (this == OWNER_OP && isOwner(source)) return true;
        return source.hasPermissions(toLevel());
    }

    public static boolean isOwner(CommandSourceStack source) {
        var server = source.getServer();
        var player = source.getPlayer();
        return server.isDedicatedServer()
            ? source.getEntity() == null && source.hasPermission(4) && source.getTextName().equals("Server")
            : player != null && server.isSingleplayerOwner(player.getGameProfile());
    }

    public static boolean isOwner(ServerPlayer player) {
        var server = player.getServer();
        return server != null && server.isSingleplayerOwner(player.getGameProfile());
    }
}
