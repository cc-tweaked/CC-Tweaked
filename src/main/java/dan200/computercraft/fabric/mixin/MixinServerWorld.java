/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.fabric.mixin;

import dan200.computercraft.shared.util.DropConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

/**
 * @see ServerWorld#spawnEntity(Entity)
 */
@Mixin (ServerWorld.class)
public class MixinServerWorld {
    @Inject (method = "spawnEntity", at = @At ("HEAD"), cancellable = true)
    public void spawnEntity(Entity entity, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (DropConsumer.onEntitySpawn(entity)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
