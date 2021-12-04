/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.SpeakerMoveClientMessage;
import dan200.computercraft.shared.network.client.SpeakerPlayClientMessage;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static dan200.computercraft.api.lua.LuaValues.checkFinite;

/**
 * Speakers allow playing notes and other sounds.
 *
 * @cc.module speaker
 * @cc.since 1.80pr1
 */
public abstract class SpeakerPeripheral implements IPeripheral
{
    private static final int MIN_TICKS_BETWEEN_SOUNDS = 1;

    private long clock = 0;
    private long lastPlayTime = 0;
    private final AtomicInteger notesThisTick = new AtomicInteger();

    private long lastPositionTime;
    private Vec3 lastPosition;

    public void update()
    {
        clock++;
        notesThisTick.set( 0 );

        // Push position updates to any speakers which have ever played a note,
        // have moved by a non-trivial amount and haven't had a position update
        // in the last second.
        if( lastPlayTime > 0 && (clock - lastPositionTime) >= 20 )
        {
            Vec3 position = getPosition();
            if( lastPosition == null || lastPosition.distanceToSqr( position ) >= 0.1 )
            {
                lastPosition = position;
                lastPositionTime = clock;
                NetworkHandler.sendToAllTracking(
                    new SpeakerMoveClientMessage( getSource(), position ),
                    getLevel().getChunkAt( new BlockPos( position ) )
                );
            }
        }
    }

    public abstract Level getLevel();

    public abstract Vec3 getPosition();

    protected abstract UUID getSource();

    public boolean madeSound( long ticks )
    {
        return clock - lastPlayTime <= ticks;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "speaker";
    }

    /**
     * Plays a sound through the speaker.
     *
     * This plays sounds similar to the {@code /playsound} command in Minecraft.
     * It takes the namespaced path of a sound (e.g. {@code minecraft:block.note_block.harp})
     * with an optional volume and speed multiplier, and plays it through the speaker.
     *
     * @param context The Lua context
     * @param name    The name of the sound to play.
     * @param volumeA The volume to play the sound at, from 0.0 to 3.0. Defaults to 1.0.
     * @param pitchA  The speed to play the sound at, from 0.5 to 2.0. Defaults to 1.0.
     * @return Whether the sound could be played.
     * @throws LuaException If the sound name couldn't be decoded.
     */
    @LuaFunction
    public final boolean playSound( ILuaContext context, String name, Optional<Double> volumeA, Optional<Double> pitchA ) throws LuaException
    {
        float volume = (float) checkFinite( 1, volumeA.orElse( 1.0 ) );
        float pitch = (float) checkFinite( 2, pitchA.orElse( 1.0 ) );

        ResourceLocation identifier;
        try
        {
            identifier = new ResourceLocation( name );
        }
        catch( ResourceLocationException e )
        {
            throw new LuaException( "Malformed sound name '" + name + "' " );
        }

        return playSound( context, identifier, volume, pitch, false );
    }

    /**
     * Plays a note block note through the speaker.
     *
     * This takes the name of a note to play, as well as optionally the volume
     * and pitch to play the note at.
     *
     * The pitch argument uses semitones as the unit. This directly maps to the
     * number of clicks on a note block. For reference, 0, 12, and 24 map to F#,
     * and 6 and 18 map to C.
     *
     * @param context The Lua context
     * @param name    The name of the note to play.
     * @param volumeA The volume to play the note at, from 0.0 to 3.0. Defaults to 1.0.
     * @param pitchA  The pitch to play the note at in semitones, from 0 to 24. Defaults to 12.
     * @return Whether the note could be played.
     * @throws LuaException If the instrument doesn't exist.
     */
    @LuaFunction
    public final synchronized boolean playNote( ILuaContext context, String name, Optional<Double> volumeA, Optional<Double> pitchA ) throws LuaException
    {
        float volume = (float) checkFinite( 1, volumeA.orElse( 1.0 ) );
        float pitch = (float) checkFinite( 2, pitchA.orElse( 1.0 ) );

        NoteBlockInstrument instrument = null;
        for( NoteBlockInstrument testInstrument : NoteBlockInstrument.values() )
        {
            if( testInstrument.getSerializedName().equalsIgnoreCase( name ) )
            {
                instrument = testInstrument;
                break;
            }
        }

        // Check if the note exists
        if( instrument == null ) throw new LuaException( "Invalid instrument, \"" + name + "\"!" );

        // If the resource location for note block notes changes, this method call will need to be updated
        boolean success = playSound( context, instrument.getSoundEvent().getLocation(), volume, (float) Math.pow( 2.0, (pitch - 12.0) / 12.0 ), true );
        if( success ) notesThisTick.incrementAndGet();
        return success;
    }

    private synchronized boolean playSound( ILuaContext context, ResourceLocation name, float volume, float pitch, boolean isNote ) throws LuaException
    {
        if( clock - lastPlayTime < MIN_TICKS_BETWEEN_SOUNDS )
        {
            // Rate limiting occurs when we've already played a sound within the last tick.
            if( !isNote ) return false;
            // Or we've played more notes than allowable within the current tick.
            if( clock - lastPlayTime != 0 || notesThisTick.get() >= ComputerCraft.maxNotesPerTick ) return false;
        }

        Level world = getLevel();
        Vec3 pos = getPosition();

        float actualVolume = Mth.clamp( volume, 0.0f, 3.0f );
        float range = actualVolume * 16;

        context.issueMainThreadTask( () -> {
            MinecraftServer server = world.getServer();
            if( server == null ) return null;

            if( isNote )
            {
                server.getPlayerList().broadcast(
                    null, pos.x, pos.y, pos.z, range, world.dimension(),
                    new ClientboundCustomSoundPacket( name, SoundSource.RECORDS, pos, actualVolume, pitch )
                );
            }
            else
            {
                NetworkHandler.sendToAllAround(
                    new SpeakerPlayClientMessage( getSource(), pos, name, actualVolume, pitch ),
                    world, pos, range
                );
            }
            return null;
        } );

        lastPlayTime = clock;
        return true;
    }
}
