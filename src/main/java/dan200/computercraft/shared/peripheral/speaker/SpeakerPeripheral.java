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
import dan200.computercraft.api.lua.LuaTable;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.SpeakerAudioClientMessage;
import dan200.computercraft.shared.network.client.SpeakerMoveClientMessage;
import dan200.computercraft.shared.network.client.SpeakerPlayClientMessage;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.properties.NoteBlockInstrument;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private static final long SECOND = TimeUnit.SECONDS.toNanos( 1 );

    /**
     * Number of samples/s in a dfpwm1a audio track.
     */
    public static final int SAMPLE_RATE = 48000;

    /**
     * The minimum size of the client's audio buffer. Once we have less than this on the client, we should send another
     * batch of audio.
     */
    public static final long CLIENT_BUFFER = (long) (SECOND * 1.5);

    private static final int MIN_TICKS_BETWEEN_SOUNDS = 1;

    private final UUID source = UUID.randomUUID();
    private final Set<IComputerAccess> computers = Collections.newSetFromMap( new HashMap<>() );

    private long clock = 0;
    private long lastPlayTime = 0;
    private final AtomicInteger notesThisTick = new AtomicInteger();

    private long lastPositionTime;
    private Vector3d lastPosition;

    private long clientEndTime = System.nanoTime();
    private float pendingVolume = 1.0f;
    private DfpwmEncoder encoder;
    private ByteBuffer pendingAudio;

    public void update()
    {
        clock++;
        notesThisTick.set( 0 );

        // If clients need to receive another batch of audio, send it and then notify computers our internal buffer is
        // free again.
        long now = System.nanoTime();
        if( pendingAudio != null && now >= clientEndTime - CLIENT_BUFFER )
        {
            Vector3d position = getPosition();
            NetworkHandler.sendToAllTracking(
                new SpeakerAudioClientMessage( getSource(), getPosition(), pendingVolume, pendingAudio ),
                getWorld().getChunkAt( new BlockPos( position ) )
            );
            syncedPosition( position );

            // Compute when we should consider sending the next packet.
            clientEndTime = Math.max( now, clientEndTime ) + (pendingAudio.remaining() * SECOND * 8 / SAMPLE_RATE);
            pendingAudio = null;

            // And notify computers that we have space for more audio.
            for( IComputerAccess computer : computers )
            {
                computer.queueEvent( "speaker_audio_empty", computer.getAttachmentName() );
            }
        }

        // Push position updates to any speakers which have ever played a note,
        // have moved by a non-trivial amount and haven't had a position update
        // in the last second.
        if( lastPosition != null && (clock - lastPositionTime) >= 20 )
        {
            Vector3d position = getPosition();
            if( lastPosition.distanceToSqr( position ) >= 0.1 )
            {
                NetworkHandler.sendToAllTracking(
                    new SpeakerMoveClientMessage( getSource(), position ),
                    getWorld().getChunkAt( new BlockPos( position ) )
                );
                syncedPosition( position );
            }
        }
    }

    @Nullable
    public abstract World getWorld();

    @Nonnull
    public abstract Vector3d getPosition();

    @Nonnull
    public UUID getSource()
    {
        return source;
    }

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
        boolean success = playSound( context, instrument.getSoundEvent().getRegistryName(), volume, (float) Math.pow( 2.0, (pitch - 12.0) / 12.0 ), true );
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

        float actualVolume = MathHelper.clamp( volume, 0.0f, 3.0f );
        float range = actualVolume * 16;

        context.issueMainThreadTask( () -> {
            World world = getWorld();
            if( world == null ) return null;

            MinecraftServer server = world.getServer();
            if( server == null ) return null;

            // We're setting the position here, so
            Vector3d pos = getPosition();

            pendingAudio = null; // Playing this sound will cause the music to stop.

            if( isNote )
            {
                server.getPlayerList().broadcast(
                    null, pos.x, pos.y, pos.z, range, world.dimension(),
                    new SPlaySoundPacket( name, SoundCategory.RECORDS, pos, actualVolume, pitch )
                );
            }
            else
            {
                NetworkHandler.sendToAllAround(
                    new SpeakerPlayClientMessage( getSource(), pos, name, actualVolume, pitch ),
                    world, pos, range
                );
            }

            syncedPosition( pos );

            return null;
        } );

        lastPlayTime = clock;
        return true;
    }

    /**
     * Attempt to stream some audio data to the speaker.
     *
     * @param context The Lua context.
     * @param audio   The audio data to play. This should be a string between 1 and 16Kib long, containing the DFPWM data to play.
     * @param volume  The volume to play this audio at.
     * @return If there was room to accept this audio data. If this returns {@literal false}, you should wait for a {@literal needs_audio}
     * event and try again.
     * @throws LuaException If the audio data is malformed.
     */
    @LuaFunction( unsafe = true )
    public final synchronized boolean playAudio( ILuaContext context, LuaTable<?, ?> audio, Optional<Double> volume ) throws LuaException
    {
        // TODO: Use ArgumentHelpers instead?
        int length = audio.length();
        if( length <= 0 ) throw new LuaException( "Cannot play empty audio" );
        if( length > 1024 * 16 * 8 ) throw new LuaException( "Audio data is too large" );
        if( pendingAudio != null ) return false;

        if( encoder == null || clientEndTime < System.nanoTime() ) encoder = new DfpwmEncoder();

        pendingAudio = encoder.encode( audio, length );
        pendingVolume = MathHelper.clamp( volume.orElse( (double) pendingVolume ).floatValue(), 0.0f, 3.0f );
        return true;
    }

    private void syncedPosition( Vector3d position )
    {
        lastPosition = position;
        lastPositionTime = clock;
    }

    @Override
    public void attach( @Nonnull IComputerAccess computer )
    {
        computers.add( computer );
    }

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        computers.remove( computer );
    }
}
