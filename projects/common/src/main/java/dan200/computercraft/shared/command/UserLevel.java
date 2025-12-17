// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.Permissions;

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

    public Permission toPermission() {
        return switch (this) {
            case OP, OWNER_OP -> Permissions.COMMANDS_GAMEMASTER;
            case ANYONE -> new Permission.HasCommandLevel(PermissionLevel.ALL);
        };
    }

    @Override
    public boolean test(CommandSourceStack source) {
        if (this == ANYONE) return true;
        if (this == OWNER_OP && isOwner(source)) return true;
        return source.permissions().hasPermission(toPermission());
    }

    public boolean test(ServerPlayer source) {
        if (this == ANYONE) return true;
        if (this == OWNER_OP && isOwner(source)) return true;
        return source.permissions().hasPermission(toPermission());
    }

    public static boolean isOwner(CommandSourceStack source) {
        var server = source.getServer();

        // While CommandSourceStack.getServer is non-nullable, that's a lie for permission checks. When loading
        // .mcfunction files, ServerFunctionLibrary constructs an instance with an empty server. In that case, return
        // false — we don't want to treat functions as an owner!
        if (server == null) return false;

        var player = source.getPlayer();
        return server.isDedicatedServer()
            ? source.getEntity() == null && source.permissions().hasPermission(Permissions.COMMANDS_ADMIN) && source.getTextName().equals("Server")
            : player != null && server.isSingleplayerOwner(player.nameAndId());
    }

    public static boolean isOwner(ServerPlayer player) {
        var server = player.level().getServer();
        return server != null && server.isSingleplayerOwner(player.nameAndId());
    }
}
