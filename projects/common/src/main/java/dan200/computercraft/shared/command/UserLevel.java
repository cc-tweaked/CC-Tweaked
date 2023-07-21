// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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

    /**
     * Take the union of two {@link UserLevel}s.
     * <p>
     * This satisfies the property that for all sources {@code s}, {@code a.test(s) || b.test(s) == (a ∪ b).test(s)}.
     *
     * @param left  The first user level to take the union of.
     * @param right The second user level to take the union of.
     * @return The union of two levels.
     */
    public static UserLevel union(UserLevel left, UserLevel right) {
        if (left == right) return left;

        // x ∪ ANYONE = ANYONE
        if (left == ANYONE || right == ANYONE) return ANYONE;

        // x ∪ OWNER = OWNER
        if (left == OWNER) return right;
        if (right == OWNER) return left;

        // At this point, we have x != y and x, y ∈ { OP, OWNER_OP }.
        return OWNER_OP;
    }

    private static boolean isOwner(CommandSourceStack source) {
        var server = source.getServer();
        var sender = source.getEntity();
        return server.isDedicatedServer()
            ? source.getEntity() == null && source.hasPermission(4) && source.getTextName().equals("Server")
            : sender instanceof Player player && server.isSingleplayerOwner(player.getGameProfile());
    }
}
