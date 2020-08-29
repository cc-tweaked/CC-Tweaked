/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.mixin;

import dan200.computercraft.shared.command.CommandCopy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;

@Mixin (Screen.class)
public class MixinScreen {
    @Inject (method = "sendMessage(Ljava/lang/String;Z)V", at = @At ("HEAD"), cancellable = true)
    public void sendClientCommand(String message, boolean add, CallbackInfo info) {
        if (CommandCopy.onClientSendMessage(message)) {
            info.cancel();
        }
    }
}
