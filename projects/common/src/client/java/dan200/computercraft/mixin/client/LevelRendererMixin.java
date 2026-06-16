// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.ClientHooks;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Provides custom block breaking progress for modems, so it only applies to the current part.
 *
 * @see LevelRenderer#submitBlockDestroyAnimation(PoseStack, SubmitNodeCollector, LevelRenderState)
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @ModifyExpressionValue(
        method = "submitBlockDestroyAnimation", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;")
    )
    @SuppressWarnings("unused")
    private Object submitBlockDestroyAnimation(Object breaking) {
        return getBlockDamageState((BlockBreakingRenderState) breaking);
    }

    @Unique
    private BlockBreakingRenderState getBlockDamageState(BlockBreakingRenderState breaking) {
        var newState = ClientHooks.getBlockBreakingState(breaking.blockState(), breaking.blockPos());
        return breaking.blockState() == newState ? breaking : new BlockBreakingRenderState(breaking.blockPos(), newState, breaking.progress());
    }
}
