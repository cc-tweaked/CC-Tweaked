/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin.client;

import dan200.computercraft.client.ClientHooks;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPacketListener.class)
class ClientPacketListenerMixin {
    @Inject(method = "sendUnsignedCommand", at = @At("HEAD"), cancellable = true)
    void commandUnsigned(String message, CallbackInfoReturnable<Boolean> ci) {
        if (ClientHooks.onChatMessage(message)) ci.setReturnValue(true);
    }
}
