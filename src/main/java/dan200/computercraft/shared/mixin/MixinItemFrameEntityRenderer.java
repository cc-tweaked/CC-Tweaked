/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.mixin;

import dan200.computercraft.client.render.ItemPrintoutRenderer;
import dan200.computercraft.shared.media.items.ItemPrintout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Mixin (ItemFrameEntityRenderer.class)
@Environment (EnvType.CLIENT)
public class MixinItemFrameEntityRenderer {
    @Inject (method = "render", at = @At ("HEAD"), cancellable = true)
    private void renderItem_Injected(ItemFrameEntity itemFrameEntity, float f, float g, MatrixStack matrixStack,
                                     VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        ItemStack stack = itemFrameEntity.getHeldItemStack();
        if (stack.getItem() instanceof ItemPrintout) {
            ItemPrintoutRenderer.INSTANCE.renderInFrame(matrixStack, vertexConsumerProvider, stack);
            info.cancel();
        }
    }
}
