/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.speaker;

import static dan200.computercraft.api.lua.LuaValues.checkFinite;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.minecraft.block.enums.Instrument;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Speakers allow playing notes and other sounds.
 *
 * @cc.module speaker
 */
public abstract class SpeakerPeripheral implements IPeripheral {
    private final AtomicInteger m_notesThisTick = new AtomicInteger();
    private long m_clock = 0;
    private long m_lastPlayTime = 0;

    public void update() {
        this.m_clock++;
        this.m_notesThisTick.set(0);
    }

    public boolean madeSound(long ticks) {
        return this.m_clock - this.m_lastPlayTime <= ticks;
    }

    @Nonnull
    @Override
    public String getType() {
        return "speaker";
    }

    /**
     * Plays a sound through the speaker.
     *
     * This plays sounds similar to the {@code /playsound} command in Minecraft. It takes the namespaced path of a sound (e.g. {@code
     * minecraft:block.note_block.harp}) with an optional volume and speed multiplier, and plays it through the speaker.
     *
     * @param context The Lua context
     * @param name The name of the sound to play.
     * @param volumeA The volume to play the sound at, from 0.0 to 3.0. Defaults to 1.0.
     * @param pitchA The speed to play the sound at, from 0.5 to 2.0. Defaults to 1.0.
     * @return Whether the sound could be played.
     * @throws LuaException If the sound name couldn't be decoded.
     */
    @LuaFunction
    public final boolean playSound(ILuaContext context, String name, Optional<Double> volumeA, Optional<Double> pitchA) throws LuaException {
        float volume = (float) checkFinite(1, volumeA.orElse(1.0));
        float pitch = (float) checkFinite(2, pitchA.orElse(1.0));

        Identifier identifier;
        try {
            identifier = new Identifier(name);
        } catch (InvalidIdentifierException e) {
            throw new LuaException("Malformed sound name '" + name + "' ");
        }

        return this.playSound(context, identifier, volume, pitch, false);
    }

    private synchronized boolean playSound(ILuaContext context, Identifier name, float volume, float pitch, boolean isNote) throws LuaException {
        if (this.m_clock - this.m_lastPlayTime < TileSpeaker.MIN_TICKS_BETWEEN_SOUNDS && (!isNote || this.m_clock - this.m_lastPlayTime != 0 || this.m_notesThisTick.get() >= ComputerCraft.maxNotesPerTick)) {
            // Rate limiting occurs when we've already played a sound within the last tick, or we've
            // played more notes than allowable within the current tick.
            return false;
        }

        World world = this.getWorld();
        Vec3d pos = this.getPosition();

        context.issueMainThreadTask(() -> {
            MinecraftServer server = world.getServer();
            if (server == null) {
                return null;
            }

            float adjVolume = Math.min(volume, 3.0f);
            server.getPlayerManager()
                  .sendToAround(null,
                                pos.x,
                                pos.y,
                                pos.z,
                                adjVolume > 1.0f ? 16 * adjVolume : 16.0,
                                world.getRegistryKey(),
                                new PlaySoundIdS2CPacket(name, SoundCategory.RECORDS, pos, adjVolume, pitch));
            return null;
        });

        this.m_lastPlayTime = this.m_clock;
        return true;
    }

    public abstract World getWorld();

    public abstract Vec3d getPosition();

    /**
     * Plays a note block note through the speaker.
     *
     * This takes the name of a note to play, as well as optionally the volume and pitch to play the note at.
     *
     * The pitch argument uses semitones as the unit. This directly maps to the number of clicks on a note block. For reference, 0, 12, and 24 map to F#,
     * and 6 and 18 map to C.
     *
     * @param context The Lua context
     * @param name The name of the note to play.
     * @param volumeA The volume to play the note at, from 0.0 to 3.0. Defaults to 1.0.
     * @param pitchA The pitch to play the note at in semitones, from 0 to 24. Defaults to 12.
     * @return Whether the note could be played.
     * @throws LuaException If the instrument doesn't exist.
     */
    @LuaFunction
    public final synchronized boolean playNote(ILuaContext context, String name, Optional<Double> volumeA, Optional<Double> pitchA) throws LuaException {
        float volume = (float) checkFinite(1, volumeA.orElse(1.0));
        float pitch = (float) checkFinite(2, pitchA.orElse(1.0));

        Instrument instrument = null;
        for (Instrument testInstrument : Instrument.values()) {
            if (testInstrument.asString()
                              .equalsIgnoreCase(name)) {
                instrument = testInstrument;
                break;
            }
        }

        // Check if the note exists
        if (instrument == null) {
            throw new LuaException("Invalid instrument, \"" + name + "\"!");
        }

        // If the resource location for note block notes changes, this method call will need to be updated
        boolean success = this.playSound(context,
                                         instrument.getSound()
                                              .getId(),
                                         volume,
                                         (float) Math.pow(2.0, (pitch - 12.0) / 12.0),
                                         true);
        if (success) {
            this.m_notesThisTick.incrementAndGet();
        }
        return success;
    }
}
