/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( ItemEntity.class )
public abstract class MixinItemEntity
{
    @Shadow
    public abstract ItemStack getItem();

    @Inject(
        method = "tick",
        at = @At( "HEAD" ),
        cancellable = true
    )
    private void onTick( CallbackInfo ci )
    {
        ItemStack stack = getItem();
        if( stack.getItem() instanceof IComputerItem item )
        {
            if( item.onEntityItemUpdate( stack, (ItemEntity) (Object) this ) ) ci.cancel();
        }
    }
}
