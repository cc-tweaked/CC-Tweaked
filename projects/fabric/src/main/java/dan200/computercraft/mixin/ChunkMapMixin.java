// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import dan200.computercraft.shared.CommonHooks;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ChunkMap.class)
class ChunkMapMixin {
    @Final
    @Shadow
    ServerLevel level;

    @Inject(method = "updateChunkScheduling", at = @At("HEAD"))
    @SuppressWarnings("UnusedMethod")
    private void onUpdateChunkScheduling(long chunkPos, int newLevel, @Nullable ChunkHolder holder, int oldLevel, CallbackInfoReturnable<ChunkHolder> callback) {
        CommonHooks.onChunkTicketLevelChanged(level, chunkPos, oldLevel, newLevel);
    }
}
