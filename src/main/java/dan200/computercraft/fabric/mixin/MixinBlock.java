/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import dan200.computercraft.shared.util.DropConsumer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Supplier;

/**
 * Captures block drops.
 *
 * @see Block#popResource(Level, Supplier, ItemStack)
 */
@Mixin( Block.class )
public class MixinBlock
{
    @Inject(
        method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/ItemEntity;setDefaultPickUpDelay()V"
        ),
        locals = LocalCapture.CAPTURE_FAILSOFT,
        cancellable = true )
    private static void dropStack( Level world, Supplier<ItemEntity> itemEntitySupplier, ItemStack stack, CallbackInfo callbackInfo, ItemEntity itemEntity )
    {
        if( DropConsumer.onHarvestDrops( world, itemEntity.blockPosition(), stack ) )
        {
            callbackInfo.cancel();
        }
    }
}
