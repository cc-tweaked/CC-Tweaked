/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.render.CableHighlightRenderer;
import dan200.computercraft.client.render.MonitorHighlightRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( LevelRenderer.class )
@Environment( EnvType.CLIENT )
public class MixinLevelRenderer
{
    @Inject( method = "renderHitOutline", cancellable = true, at = @At( "HEAD" ) )
    public void renderHitOutline( PoseStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos,
                                  BlockState blockState, CallbackInfo info )
    {
        if( CableHighlightRenderer.drawHighlight( matrixStack, vertexConsumer, entity, d, e, f, blockPos, blockState )
            || MonitorHighlightRenderer.drawHighlight( matrixStack, vertexConsumer, entity, d, e, f, blockPos, blockState ) )
        {
            info.cancel();
        }
    }
}
