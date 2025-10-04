// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.ClientHooks;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Provides custom block breaking progress for modems, so it only applies to the current part.
 *
 * @see BlockRenderDispatcher#renderBreakingTexture(BlockState, BlockPos, BlockAndTintGetter, PoseStack, VertexConsumer)
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {
    @ModifyVariable(method = "renderBreakingTexture", at = @At("HEAD"))
    public BlockState renderBlockDamage(BlockState state, @Local BlockPos pos) {
        return ClientHooks.getBlockBreakingState(state, pos);
    }
}
