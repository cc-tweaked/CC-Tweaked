// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import dan200.computercraft.shared.CommonHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
class PlayerChunkSenderMixin {
    @Inject(method = "sendChunk", at = @At("TAIL"))
    @SuppressWarnings("UnusedMethod")
    private static void onPlayerLoadedChunk(ServerGamePacketListenerImpl connection, ServerLevel server, LevelChunk chunk, CallbackInfo ci) {
        CommonHooks.onChunkWatch(chunk, connection.player);
    }
}
