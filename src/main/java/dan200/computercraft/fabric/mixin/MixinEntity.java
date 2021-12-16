/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import dan200.computercraft.shared.util.DropConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures entities.
 *
 * @see Entity#spawnAtLocation(ItemStack, float)
 */
@Mixin( Entity.class )
public class MixinEntity
{
    @Inject( method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At( value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z" ),
        cancellable = true )
    public void spawnAtLocation( ItemStack stack, float height, CallbackInfoReturnable<ItemEntity> callbackInfo )
    {
        if( DropConsumer.onLivingDrops( (Entity) (Object) this, stack ) )
        {
            callbackInfo.setReturnValue( null );
        }
    }
}
