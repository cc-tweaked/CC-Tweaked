/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin.client;

import dan200.computercraft.client.ClientRegistry;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

@Mixin(GameRenderer.class)
class GameRendererMixin {
    @Final
    @Shadow
    @SuppressWarnings("NullAway")
    private Map<String, ShaderInstance> shaders;

    @Inject(method = "reloadShaders", at = @At(value = "TAIL"))
    @SuppressWarnings("UnusedMethod")
    private void onReloadShaders(ResourceManager resourceManager, CallbackInfo ci) {
        try {
            ClientRegistry.registerShaders(resourceManager, (shader, callback) -> {
                shaders.put(shader.getName(), shader);
                callback.accept(shader);
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not reload shaders", e);
        }
    }
}
