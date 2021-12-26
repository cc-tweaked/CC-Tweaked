/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import dan200.computercraft.fabric.events.CustomServerEvents;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( ChunkMap.class )
public class MixinChunkMap
{
    @Final
    @Shadow
    ServerLevel level;

    /*
    * This mixin mimics the logic of Forge's ChunkWatchEvent.Watch but I don't believe it behaves as expected. Instead
    * of firing once when the player initially come in server view distance of a chunk, it fires every time
    * the chunk is checked against the player's view distance. This continually happens every tick that the player
    * moves (or even rotates in place).
    */

    //    @Inject( method = "updateChunkTracking", at = @At( value = "HEAD" ) )
    //    public void updateChunkTracking( ServerPlayer serverPlayer, ChunkPos chunkPos, MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject, boolean bl, boolean bl2, CallbackInfo ci )
    //    {
    //        if( serverPlayer.level == this.level && bl )
    //        {
    //            CustomServerEvents.SERVER_PLAYER_LOADED_CHUNK_EVENT.invoker().onServerPlayerLoadedChunk( serverPlayer, chunkPos );
    //        }
    //    }

    // This version behaves as expected in my testing.
    @Inject( method = "playerLoadedChunk", at = @At( value = "HEAD" ) )
    private void playerLoadedChunk( ServerPlayer serverPlayer, MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject, LevelChunk levelChunk, CallbackInfo ci )
    {
        CustomServerEvents.SERVER_PLAYER_LOADED_CHUNK_EVENT.invoker().onServerPlayerLoadedChunk( serverPlayer, levelChunk.getPos() );
    }

}
