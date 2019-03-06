/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.mixin;

import dan200.computercraft.client.render.ItemPocketRenderer;
import dan200.computercraft.client.render.ItemPrintoutRenderer;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.mixed.MixedFirstPersonRenderer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.FirstPersonRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.sortme.OptionMainHand;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( FirstPersonRenderer.class )
public class MixinFirstPersonRenderer implements MixedFirstPersonRenderer
{
    @Shadow
    private float method_3227( float pitch )
    {
        return 0;
    }

    @Shadow
    private void renderArms()
    {
    }

    @Shadow
    private void method_3219( float equip, float swing, OptionMainHand hand )
    {
    }

    @Override
    public void renderArms_CC()
    {
        renderArms();
    }

    @Override
    public void renderArmFirstPerson_CC( float equip, float swing, OptionMainHand hand )
    {
        method_3219( equip, swing, hand );
    }

    @Override
    public float getMapAngleFromPitch_CC( float pitch )
    {
        return method_3227( pitch );
    }

    @Inject(
        method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;F)V",
        at = @At( "HEAD" ),
        cancellable = true
    )
    public void renderFirstPersonItem_Injected( AbstractClientPlayerEntity player, float var2, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, CallbackInfo callback )
    {
        if( stack.getItem() instanceof ItemPrintout )
        {
            ItemPrintoutRenderer.INSTANCE.renderItemFirstPerson( hand, pitch, equipProgress, swingProgress, stack );
            callback.cancel();
        }
        else if( stack.getItem() instanceof ItemPocketComputer )
        {
            ItemPocketRenderer.INSTANCE.renderItemFirstPerson( hand, pitch, equipProgress, swingProgress, stack );
            callback.cancel();
        }
    }
}
