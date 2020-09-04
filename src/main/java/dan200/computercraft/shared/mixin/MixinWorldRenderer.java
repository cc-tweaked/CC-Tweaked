/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.mixin;

import dan200.computercraft.client.render.CableHighlightRenderer;
import dan200.computercraft.client.render.MonitorHighlightRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Mixin (WorldRenderer.class)
@Environment (EnvType.CLIENT)
public class MixinWorldRenderer {
    @Inject (method = "drawBlockOutline", cancellable = true, at = @At ("HEAD"))
    public void drawBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos,
                                 BlockState blockState, CallbackInfo info) {
        if (CableHighlightRenderer.drawHighlight(matrixStack,
                                                 vertexConsumer,
                                                 entity,
                                                 d,
                                                 e,
                                                 f,
                                                 blockPos,
                                                 blockState) || MonitorHighlightRenderer.drawHighlight(matrixStack,
                                                                                                       vertexConsumer,
                                                                                                       entity,
                                                                                                       d,
                                                                                                       e,
                                                                                                       f,
                                                                                                       blockPos,
                                                                                                       blockState)) {
            info.cancel();
        }
    }
}
