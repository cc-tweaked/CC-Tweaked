/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin.client;

import dan200.computercraft.client.ClientHooks;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
class DebugScreenOverlayMixin {
    @Inject(method = "getSystemInformation", at = @At("RETURN"))
    @SuppressWarnings("UnusedMethod")
    private void appendBlockDebugInfo(CallbackInfoReturnable<List<String>> cir) {
        ClientHooks.addBlockDebugInfo(cir.getReturnValue()::add);
    }

    @Inject(method = "getGameInformation", at = @At("RETURN"))
    @SuppressWarnings("UnusedMethod")
    private void appendGameDebugInfo(CallbackInfoReturnable<List<String>> cir) {
        ClientHooks.addGameDebugInfo(cir.getReturnValue()::add);
    }
}
