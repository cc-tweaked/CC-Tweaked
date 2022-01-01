/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import dan200.computercraft.shared.network.client.SpeakerStopClientMessage;
import dan200.computercraft.shared.util.PauseAwareTimer;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static dan200.computercraft.api.lua.LuaValues.checkFinite;

/**
 * The speaker peirpheral allow your computer to play notes and other sounds.
 *
 * The speaker can play three kinds of sound, in increasing orders of complexity:
 * - {@link #playNote} allows you to play noteblock note.
 * - {@link #playSound} plays any built-in Minecraft sound, such as block sounds or mob noises.
 * - {@link #playAudio} can play arbitrary audio.
 *
 * @cc.module speaker
 * @cc.since 1.80pr1
 */
public abstract class SpeakerPeripheral implements IPeripheral
{
    /**
     * Number of samples/s in a dfpwm1a audio track.
     */
    public static final int SAMPLE_RATE = 48000;

    private final UUID source = UUID.randomUUID();
    private final Set<IComputerAccess> computers = new HashSet<>();

    private long clock = 0;
    private long lastPositionTime;
    private Vec3 lastPosition;

    private long lastPlayTime;

    private final List<PendingSound> pendingNotes = new ArrayList<>();

    private final Object lock = new Object();
    private boolean shouldStop;
    private PendingSound pendingSound = null;
    private DfpwmState dfpwmState;

    public void update()
    {
        clock++;

        Vec3 pos = getPosition();
        Level level = getLevel();
        if( level == null ) return;
        MinecraftServer server = level.getServer();

        synchronized( pendingNotes )
        {
            for( PendingSound sound : pendingNotes )
            {
                lastPlayTime = clock;
                server.getPlayerList().broadcast(
                    null, pos.x, pos.y, pos.z, sound.volume * 16, level.dimension(),
                    new ClientboundCustomSoundPacket( sound.location, SoundSource.RECORDS, pos, sound.volume, sound.pitch )
                );
            }
            pendingNotes.clear();
        }

        // The audio dispatch logic here is pretty messy, which I'm not proud of. The general logic here is that we hold
        // the main "lock" when modifying the dfpwmState/pendingSound variables and no other time.
        // dfpwmState will only ever transition from having a buffer to not having a buffer on the main thread (so this
        // method), so we don't need to bother locking that.
        boolean shouldStop;
        PendingSound sound;
        DfpwmState dfpwmState;
        synchronized( lock )
        {
            sound = pendingSound;
            dfpwmState = this.dfpwmState;
            pendingSound = null;

            shouldStop = this.shouldStop;
            if( shouldStop )
            {
                dfpwmState = this.dfpwmState = null;
                sound = null;
                this.shouldStop = false;
            }
        }

        // Stop the speaker and nuke the position, so we don't update it again.
        if( shouldStop && lastPosition != null )
        {
            lastPosition = null;
            NetworkHandler.sendToAllPlayers( new SpeakerStopClientMessage( getSource() ) );
            return;
        }

        long now = PauseAwareTimer.getTime();
        if( sound != null )
        {
            lastPlayTime = clock;
            NetworkHandler.sendToAllAround(
                new SpeakerPlayClientMessage( getSource(), pos, sound.location, sound.volume, sound.pitch ),
                level, pos, sound.volume * 16
            );
            syncedPosition( pos );
        }
        else if( dfpwmState != null && dfpwmState.shouldSendPending( now ) )
        {
            // If clients need to receive another batch of audio, send it and then notify computers our internal buffer is
            // free again.
            NetworkHandler.sendToAllTracking(
                new SpeakerAudioClientMessage( getSource(), pos, dfpwmState.getVolume(), dfpwmState.pullPending( now ) ),
                getLevel().getChunkAt( new BlockPos( pos ) )
            );
            syncedPosition( pos );

            // And notify computers that we have space for more audio.
            synchronized( computers )
            {
                for( IComputerAccess computer : computers )
                {
                    computer.queueEvent( "speaker_audio_empty", computer.getAttachmentName() );
                }
            }
        }

        // Push position updates to any speakers which have ever played a note,
        // have moved by a non-trivial amount and haven't had a position update
        // in the last second.
        if( lastPosition != null && (clock - lastPositionTime) >= 20 )
        {
            Vec3 position = getPosition();
            if( lastPosition.distanceToSqr( position ) >= 0.1 )
            {
                NetworkHandler.sendToAllTracking(
                    new SpeakerMoveClientMessage( getSource(), position ),
                    getLevel().getChunkAt( new BlockPos( position ) )
                );
                syncedPosition( position );
            }
        }
    }

    @Nullable
    public abstract Level getLevel();

    @Nonnull
    public abstract Vec3 getPosition();

    @Nonnull
    public UUID getSource()
    {
        return source;
    }

    public boolean madeSound()
    {
        DfpwmState state = dfpwmState;
        return clock - lastPlayTime <= 20 || (state != null && state.isPlaying());
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "speaker";
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
     * A maximum of 8 notes can be played in a single tick. If this limit is hit, this function will return
     * {@literal false}.
     *
     * ### Valid instruments
     * The speaker supports [all of Minecraft's noteblock instruments](https://minecraft.fandom.com/wiki/Note_Block#Instruments).
     * These are:
     *
     * {@code "harp"}, {@code "basedrum"}, {@code "snare"}, {@code "hat"}, {@code "bass"}, @code "flute"},
     * {@code "bell"}, {@code "guitar"}, {@code "chime"}, {@code "xylophone"}, {@code "iron_xylophone"},
     * {@code "cow_bell"}, {@code "didgeridoo"}, {@code "bit"}, {@code "banjo"} and {@code "pling"}.
     *
     * @param context     The Lua context
     * @param instrumentA The instrument to use to play this note.
     * @param volumeA     The volume to play the note at, from 0.0 to 3.0. Defaults to 1.0.
     * @param pitchA      The pitch to play the note at in semitones, from 0 to 24. Defaults to 12.
     * @return Whether the note could be played as the limit was reached.
     * @throws LuaException If the instrument doesn't exist.
     */
    @LuaFunction
    public final boolean playNote( ILuaContext context, String instrumentA, Optional<Double> volumeA, Optional<Double> pitchA ) throws LuaException
    {
        float volume = (float) checkFinite( 1, volumeA.orElse( 1.0 ) );
        float pitch = (float) checkFinite( 2, pitchA.orElse( 1.0 ) );

        NoteBlockInstrument instrument = null;
        for( NoteBlockInstrument testInstrument : NoteBlockInstrument.values() )
        {
            if( testInstrument.getSerializedName().equalsIgnoreCase( instrumentA ) )
            {
                instrument = testInstrument;
                break;
            }
        }

        // Check if the note exists
        if( instrument == null ) throw new LuaException( "Invalid instrument, \"" + instrument + "\"!" );

        synchronized( pendingNotes )
        {
            if( pendingNotes.size() >= ComputerCraft.maxNotesPerTick ) return false;
            pendingNotes.add( new PendingSound( instrument.getSoundEvent().getRegistryName(), volume, (float) Math.pow( 2.0, (pitch - 12.0) / 12.0 ) ) );
        }
        return true;
    }

    /**
     * Plays a Minecraft sound through the speaker.
     *
     * This takes the [name of a Minecraft sound](https://minecraft.fandom.com/wiki/Sounds.json), such as
     * {@code "minecraft:block.note_block.harp"}, as well as an optional volume and pitch.
     *
     * Only one sound can be played at once. This function will return {@literal false} if another sound was started
     * this tick, or if some {@link #playAudio audio} is still playing.
     *
     * @param context The Lua context
     * @param name    The name of the sound to play.
     * @param volumeA The volume to play the sound at, from 0.0 to 3.0. Defaults to 1.0.
     * @param pitchA  The speed to play the sound at, from 0.5 to 2.0. Defaults to 1.0.
     * @return Whether the sound could be played.
     * @throws LuaException If the sound name was invalid.
     * @cc.usage Play a creeper hiss with the speaker.
     *
     * <pre>{@code
     * local speaker = peripheral.find("speaker")
     * speaker.playSound("entity.creeper.primed")
     * }</pre>
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

        synchronized( lock )
        {
            if( dfpwmState != null && dfpwmState.isPlaying() ) return false;
            dfpwmState = null;
            pendingSound = new PendingSound( identifier, volume, pitch );
            return true;
        }
    }

    /**
     * Attempt to stream some audio data to the speaker.
     *
     * This accepts a list of audio samples as amplitudes between -128 and 127. These are stored in an internal buffer
     * and played back at 48kHz. If this buffer is full, this function will return {@literal false}. You should wait for
     * a @{speaker_audio_empty} event before trying again.
     *
     * :::note
     * The speaker only buffers a single call to {@link #playAudio} at once. This means if you try to play a small
     * number of samples, you'll have a lot of stutter. You should try to play as many samples in one call as possible
     * (up to 128×1024), as this reduces the chances of audio stuttering or halting, especially when the server or
     * computer is lagging.
     * :::
     *
     * {@literal @}{speaker_audio} provides a more complete guide in to using speakers
     *
     * @param context The Lua context.
     * @param audio   The audio data to play.
     * @param volume  The volume to play this audio at.
     * @return If there was room to accept this audio data.
     * @throws LuaException If the audio data is malformed.
     * @cc.tparam {number...} audio A list of amplitudes.
     * @cc.tparam [opt] number volume The volume to play this audio at. If not given, defaults to the previous volume
     * given to {@link #playAudio}.
     * @cc.since 1.100
     * @cc.usage Read an audio file, decode it using @{cc.audio.dfpwm}, and play it using the speaker.
     *
     * <pre>{@code
     * local dfpwm = require("cc.audio.dfpwm")
     * local speaker = peripheral.find("speaker")
     *
     * local decoder = dfpwm.make_decoder()
     * for chunk in io.lines("data/example.dfpwm", 16 * 1024) do
     *     local buffer = decoder(chunk)
     *
     *     while not speaker.playAudio(buffer) do
     *         os.pullEvent("speaker_audio_empty")
     *     end
     * end
     * }</pre>
     * @cc.see cc.audio.dfpwm Provides utilities for decoding DFPWM audio files into a format which can be played by
     * the speaker.
     * @cc.see speaker_audio For a more complete introduction to the {@link #playAudio} function.
     */
    @LuaFunction( unsafe = true )
    public final boolean playAudio( ILuaContext context, LuaTable<?, ?> audio, Optional<Double> volume ) throws LuaException
    {
        checkFinite( 1, volume.orElse( 0.0 ) );

        // TODO: Use ArgumentHelpers instead?
        int length = audio.length();
        if( length <= 0 ) throw new LuaException( "Cannot play empty audio" );
        if( length > 128 * 1024 ) throw new LuaException( "Audio data is too large" );

        DfpwmState state;
        synchronized( lock )
        {
            if( dfpwmState == null || !dfpwmState.isPlaying() ) dfpwmState = new DfpwmState();
            state = dfpwmState;

            pendingSound = null;
        }

        return state.pushBuffer( audio, length, volume );
    }

    /**
     * Stop all audio being played by this speaker.
     *
     * This clears any audio that {@link #playAudio} had queued and stops the latest sound played by {@link #playSound}.
     *
     * @cc.since 1.100
     */
    @LuaFunction
    public final void stop()
    {
        shouldStop = true;
    }

    private void syncedPosition( Vec3 position )
    {
        lastPosition = position;
        lastPositionTime = clock;
    }

    @Override
    public void attach( @Nonnull IComputerAccess computer )
    {
        synchronized( computers )
        {
            computers.add( computer );
        }
    }

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        synchronized( computers )
        {
            computers.remove( computer );
        }
    }

    private record PendingSound(ResourceLocation location, float volume, float pitch)
    {
    }
}
