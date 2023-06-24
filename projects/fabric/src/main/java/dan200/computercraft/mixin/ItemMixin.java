// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import dan200.computercraft.shared.platform.FakePlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Item.class)
class ItemMixin {
    /**
     * Replace the reach distance in {@link Item#getPlayerPOVHitResult(Level, Player, ClipContext.Fluid)}.
     *
     * @param reach  The original reach distance.
     * @param level  The current level.
     * @param player The current player.
     * @return The new reach distance.
     * @see FakePlayer#getBlockReach()
     */
    @ModifyConstant(method = "getPlayerPOVHitResult", constant = @Constant(doubleValue = 5))
    @SuppressWarnings("UnusedMethod")
    private static double getReachDistance(double reach, Level level, Player player) {
        return player instanceof FakePlayer fp ? fp.getBlockReach() : reach;
    }
}
