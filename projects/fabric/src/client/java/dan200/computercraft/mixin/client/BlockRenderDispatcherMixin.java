// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.ClientHooks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides custom block breaking progress for modems, so it only applies to the current part.
 *
 * @see BlockRenderDispatcher#renderBreakingTexture(BlockState, BlockPos, BlockAndTintGetter, PoseStack, VertexConsumer)
 */
@Mixin(BlockRenderDispatcher.class)
class BlockRenderDispatcherMixin {
    @Shadow
    @Final
    private RandomSource random;

    @Shadow
    @Final
    private BlockModelShaper blockModelShaper;

    @Shadow
    @Final
    private ModelBlockRenderer modelRenderer;

    @Inject(
        method = "renderBreakingTexture",
        at = @At("HEAD"),
        cancellable = true,
        require = 0 // This isn't critical functionality, so don't worry if we can't apply it.
    )
    @SuppressWarnings("UnusedMethod")
    private void renderBlockDamage(
        BlockState state, BlockPos pos, BlockAndTintGetter world, PoseStack pose, VertexConsumer buffers,
        CallbackInfo info
    ) {
        var newState = ClientHooks.getBlockBreakingState(state, pos);
        if (newState != null) {
            info.cancel();

            var model = blockModelShaper.getBlockModel(newState);
            modelRenderer.tesselateBlock(
                world, model, newState, pos, pose, buffers, true, random, newState.getSeed(pos),
                OverlayTexture.NO_OVERLAY
            );
        }
    }
}
