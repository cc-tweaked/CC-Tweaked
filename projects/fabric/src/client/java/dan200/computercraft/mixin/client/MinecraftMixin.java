// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import dan200.computercraft.client.ClientHooks;
import dan200.computercraft.client.ClientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
class MinecraftMixin {
    @Shadow
    @Final
    private ReloadableResourceManager resourceManager;

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    @SuppressWarnings("unused")
    private void clearLevel(Screen screen, CallbackInfo ci) {
        ClientHooks.onWorldUnload();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    @SuppressWarnings("unused")
    private void setLevel(ClientLevel screen, CallbackInfo ci) {
        ClientHooks.onWorldUnload();
    }

    @Inject(
        method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/ResourceLoadStateTracker;startReload(Lnet/minecraft/client/ResourceLoadStateTracker$ReloadReason;Ljava/util/List;)V",
            ordinal = 0
        )
    )
    @SuppressWarnings("unused")
    public void beforeInitialResourceReload(GameConfig gameConfig, CallbackInfo ci) {
        ClientRegistry.registerReloadListeners(resourceManager::registerReloadListener, (Minecraft) (Object) this);
    }
}
