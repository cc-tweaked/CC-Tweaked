// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest.client;

import dan200.computercraft.gametest.core.MinecraftExtensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(Minecraft.class)
class MinecraftMixin implements MinecraftExtensions {
    @Final
    @Shadow
    public LevelRenderer levelRenderer;

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Unique
    private final AtomicBoolean isStable = new AtomicBoolean(false);

    @Inject(method = "runTick", at = @At("TAIL"))
    @SuppressWarnings("unused")
    private void updateStable(boolean render, CallbackInfo ci) {
        isStable.set(
            level != null && player != null &&
                levelRenderer.isChunkCompiled(player.blockPosition()) && levelRenderer.countRenderedChunks() > 10 &&
                levelRenderer.hasRenderedAllChunks()
        );
    }

    @Override
    public boolean computercraft$isRenderingStable() {
        return isStable.get();
    }
}
