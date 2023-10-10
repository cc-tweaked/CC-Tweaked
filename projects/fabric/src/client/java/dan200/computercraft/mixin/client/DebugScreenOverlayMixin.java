// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
