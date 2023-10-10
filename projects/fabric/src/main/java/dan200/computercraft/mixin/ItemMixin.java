// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import dan200.computercraft.shared.platform.FakePlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
class ItemMixin {
    /**
     * Replace the reach distance in {@link Item#getPlayerPOVHitResult(Level, Player, ClipContext.Fluid)}.
     *
     * @param level     The current level.
     * @param player    The current player.
     * @param fluidMode The current clip-context fluid mode.
     * @param cir       Callback info to store the new reach distance.
     * @see FakePlayer#getBlockReach()
     */
    @Inject(method = "getPlayerPOVHitResult", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("UnusedMethod")
    private static void getReachDistance(Level level, Player player, ClipContext.Fluid fluidMode, CallbackInfoReturnable<BlockHitResult> cir) {
        // It would theoretically be cleaner to use @ModifyConstant here, but as it's treated as a @Redirect, it doesn't
        // compose with other mods. Instead, we replace the method when working with our fake player.
        if (player instanceof FakePlayer fp) cir.setReturnValue(getHitResult(level, fp, fluidMode));
    }

    @Unique
    private static BlockHitResult getHitResult(Level level, FakePlayer player, ClipContext.Fluid fluidMode) {
        var start = player.getEyePosition();
        var reach = player.getBlockReach();
        var direction = player.getViewVector(1.0f);
        var end = start.add(direction.x() * reach, direction.y() * reach, direction.z() * reach);
        return level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, fluidMode, player));
    }
}
