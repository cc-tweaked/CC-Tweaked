// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
