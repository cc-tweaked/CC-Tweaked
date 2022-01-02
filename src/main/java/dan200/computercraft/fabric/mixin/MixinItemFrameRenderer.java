/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.ItemPrintoutRenderer;
import dan200.computercraft.shared.media.items.ItemPrintout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( ItemFrameRenderer.class )
@Environment( EnvType.CLIENT )
public class MixinItemFrameRenderer
{
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lcom/mojang/math/Quaternion;)V",
            ordinal = 2,
            shift = At.Shift.AFTER
        ),
        cancellable = true )
    private void renderItem(
        ItemFrame itemFrameEntity, float f, float g, PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider, int itemFrameEntityLight, CallbackInfo info
    )
    {
        ItemStack stack = itemFrameEntity.getItem();
        if( stack.getItem() instanceof ItemPrintout )
        {
            int light = itemFrameEntity.getType() == EntityType.GLOW_ITEM_FRAME ? 0xf000d2 : itemFrameEntityLight; // See getLightVal.
            ItemPrintoutRenderer.INSTANCE.renderInFrame( matrixStack, vertexConsumerProvider, stack, light );
            // TODO: need to find how to make if statement instead return, like it doing Forge
            matrixStack.popPose();
            info.cancel();
        }
    }
}
