/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.mixin;

import dan200.computercraft.client.render.CableHighlightRenderer;
import dan200.computercraft.client.render.MonitorHighlightRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

@Mixin (WorldRenderer.class)
public class MixinWorldRenderer {
    @Inject (method = "drawHighlightedBlockOutline", cancellable = true, at = @At ("HEAD"))
    public void drawHighlightedBlockOutline(Camera camera, HitResult hit, int flag, CallbackInfo info) {
        if (flag != 0 || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        if (CableHighlightRenderer.drawHighlight(camera, blockHit) || MonitorHighlightRenderer.drawHighlight(camera, blockHit)) {
            info.cancel();
        }
    }
}
