// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
    @Inject(method = "updateLevelInEngines", at = @At("HEAD"))
    @SuppressWarnings("unused")
    private void updateLevelInEngines(ClientLevel screen, CallbackInfo ci) {
        ClientHooks.onWorldUnload();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    @SuppressWarnings("unused")
    private void disconnect(Screen screen, boolean keepResourcePacks, CallbackInfo ci) {
        ClientHooks.onDisconnect();
    }
}
