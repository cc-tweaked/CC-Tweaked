// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import dan200.computercraft.shared.command.CommandComputerCraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraftforge.client.ClientCommandHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows triggering ComputerCraft's client commands from chat components events.
 */
@Mixin(ClientPacketListener.class)
class ClientPacketListenerMixin {
    @Inject(method = "sendUnsignedCommand", at = @At("HEAD"), cancellable = true)
    void commandUnsigned(String command, CallbackInfoReturnable<Boolean> ci) {
        if (command.startsWith(CommandComputerCraft.CLIENT_OPEN_FOLDER) && ClientCommandHandler.runCommand(command)) {
            ci.setReturnValue(true);
        }
    }
}
