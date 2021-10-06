/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import dan200.computercraft.client.render.ItemPrintoutRenderer;
import dan200.computercraft.shared.media.items.ItemPrintout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( ItemFrameEntityRenderer.class )
@Environment( EnvType.CLIENT )
public class MixinItemFrameEntityRenderer
{
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lnet/minecraft/util/math/Quaternion;)V",
            ordinal = 2,
            shift = At.Shift.AFTER
        ),
        cancellable = true )
    private void renderItem(
        ItemFrameEntity itemFrameEntity, float f, float g, MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider, int itemFrameEntityLight, CallbackInfo info
    )
    {
        ItemStack stack = itemFrameEntity.getHeldItemStack();
        if( stack.getItem() instanceof ItemPrintout )
        {
            int light = itemFrameEntity.getType() == EntityType.GLOW_ITEM_FRAME ? 0xf000d2 : itemFrameEntityLight; // See getLightVal.
            ItemPrintoutRenderer.INSTANCE.renderInFrame( matrixStack, vertexConsumerProvider, stack, light );
            // TODO: need to find how to make if instead return, like it doing Forge
            matrixStack.pop();
            info.cancel();
        }
    }
}
