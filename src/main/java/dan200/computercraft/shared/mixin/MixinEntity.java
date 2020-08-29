/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.mixin;

import dan200.computercraft.shared.util.DropConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;

/**
 * @see Entity#dropStack(ItemStack, float)
 */
@Mixin (Entity.class)
public class MixinEntity {
    @Inject (method = "dropStack(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;",
        at = @At (value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"),
        cancellable = true)
    public void dropStack(ItemStack stack, float height, CallbackInfoReturnable<ItemEntity> callbackInfo) {
        if (DropConsumer.onEntityLivingDrops((Entity) (Object) this, stack)) {
            callbackInfo.setReturnValue(null);
        }
    }
}
