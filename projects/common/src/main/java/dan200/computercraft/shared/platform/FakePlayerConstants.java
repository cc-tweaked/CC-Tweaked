// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Shared constants for {@linkplain PlatformHelper#createFakePlayer(ServerLevel, GameProfile) fake player}
 * implementations.
 *
 * @see net.minecraft.server.level.ServerPlayer
 * @see net.minecraft.world.entity.player.Player
 */
final class FakePlayerConstants {
    private FakePlayerConstants() {
    }

    /**
     * The maximum distance this player can reach.
     * <p>
     * This is used in the override of {@link net.minecraft.world.entity.player.Player#mayUseItemAt(BlockPos, Direction, ItemStack)},
     * to prevent the fake player reaching more than 2 blocks away.
     */
    static final double MAX_REACH = 2;
}
