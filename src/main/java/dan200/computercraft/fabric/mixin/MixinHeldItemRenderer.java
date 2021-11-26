/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.ItemPocketRenderer;
import dan200.computercraft.client.render.ItemPrintoutRenderer;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( ItemInHandRenderer.class )
@Environment( EnvType.CLIENT )
public class MixinHeldItemRenderer
{
    @Shadow
    private void renderArmHoldingItem( PoseStack matrices, MultiBufferSource vertexConsumers, int light, float equipProgress, float swingProgress,
                                       HumanoidArm arm )
    {
    }

    @Shadow
    private float getMapAngle( float pitch )
    {
        return 0;
    }

    @Inject( method = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;" +
        "FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;" +
        "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At( "HEAD" ),
        cancellable = true )
    public void renderFirstPersonItem(
        AbstractClientPlayer player, float var2, float pitch, InteractionHand hand, float swingProgress,
        ItemStack stack, float equipProgress, PoseStack matrixStack, MultiBufferSource provider, int light,
        CallbackInfo callback
    )
    {
        if( stack.getItem() instanceof ItemPrintout )
        {
            ItemPrintoutRenderer.INSTANCE.renderItemFirstPerson( matrixStack, provider, light, hand, pitch, equipProgress, swingProgress, stack );
            callback.cancel();
        }
        else if( stack.getItem() instanceof ItemPocketComputer )
        {
            ItemPocketRenderer.INSTANCE.renderItemFirstPerson( matrixStack, provider, light, hand, pitch, equipProgress, swingProgress, stack );
            callback.cancel();
        }
    }
}
