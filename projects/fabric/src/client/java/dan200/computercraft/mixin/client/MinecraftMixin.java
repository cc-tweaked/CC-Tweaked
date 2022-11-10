/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin.client;

import dan200.computercraft.client.ClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
class MinecraftMixin {
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    @SuppressWarnings("UnusedMethod")
    private void clearLevel(Screen screen, CallbackInfo ci) {
        ClientHooks.onWorldUnload();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    @SuppressWarnings("UnusedMethod")
    private void setLevel(ClientLevel screen, CallbackInfo ci) {
        ClientHooks.onWorldUnload();
    }
}
