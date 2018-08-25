/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2016. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;
import static dan200.computercraft.core.apis.ArgumentHelper.optReal;

public class SpeakerPeripheral implements IPeripheral {
    private TileSpeaker m_speaker;
    private long m_clock;
    private long m_lastPlayTime;
    private final AtomicInteger m_notesThisTick;

    public SpeakerPeripheral()
    {
        m_clock = 0;
        m_lastPlayTime = 0;
        m_notesThisTick = new AtomicInteger(  );
    }

    SpeakerPeripheral(TileSpeaker speaker)
    {
        this();
        m_speaker = speaker;
    }

    public void update()
    {
        m_clock++;
        m_notesThisTick.set( 0 );
    }

    public World getWorld()
    {
        return m_speaker.getWorld();
    }

    public BlockPos getPos()
    {
        return m_speaker.getPos();
    }

    public boolean madeSound(long ticks)
    {
        return m_clock - m_lastPlayTime <= ticks;
    }

    /* IPeripheral implementation */

    @Override
    public boolean equals( IPeripheral other )
    {
        if( other != null && other instanceof SpeakerPeripheral )
        {
            SpeakerPeripheral otherSpeaker = (SpeakerPeripheral) other;
            return otherSpeaker.m_speaker == m_speaker;
        }
        else
        {
            return false;
        }
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "speaker";
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
                "playSound", // Plays sound at resourceLocator
                "playNote" // Plays note
        };
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computerAccess, @Nonnull ILuaContext context, int methodIndex, @Nonnull Object[] args ) throws LuaException
    {
        switch( methodIndex )
        {
            // playSound
            case 0:
            {
                return playSound(args, context, false);
            }

            // playNote
            case 1:
            {
                return playNote(args, context);
            }

            default:
            {
                throw new LuaException("Method index out of range!");
            }

        }
    }

    @Nonnull
    private synchronized Object[] playNote( Object[] arguments, ILuaContext context ) throws LuaException
    {
        String name = getString(arguments, 0);
        float volume = (float) optReal( arguments, 1, 1.0 );
        float pitch = (float) optReal( arguments, 2, 1.0 );

        // Check if sound exists
        if ( !SoundEvent.REGISTRY.containsKey( new ResourceLocation( "block.note." + name ) ) )
        {
            throw new LuaException("Invalid instrument, \"" + arguments[0] + "\"!");
        }

        // If the resource location for note block notes changes, this method call will need to be updated
        Object[] returnValue = playSound(
            new Object[] {
                "block.note." + name,
                (double)Math.min( volume, 3f ),
                Math.pow( 2.0f, ( pitch - 12.0f ) / 12.0f)
            }, context, true
        );

        if( returnValue[0] instanceof Boolean && (Boolean) returnValue[0] )
        {
            m_notesThisTick.incrementAndGet();
        }

        return returnValue;
    }

    @Nonnull
    private synchronized Object[] playSound( Object[] arguments, ILuaContext context, boolean isNote ) throws LuaException
    {
        String name = getString(arguments, 0);
        float volume = (float) optReal( arguments, 1, 1.0 );
        float pitch = (float) optReal( arguments, 2, 1.0 );

        ResourceLocation resourceName = new ResourceLocation( name );

        if( m_clock - m_lastPlayTime >= TileSpeaker.MIN_TICKS_BETWEEN_SOUNDS || (isNote && m_clock - m_lastPlayTime == 0 && m_notesThisTick.get() < ComputerCraft.maxNotesPerTick) )
        {
            if( SoundEvent.REGISTRY.containsKey(resourceName) )
            {
                final World world = getWorld();
                final BlockPos pos = getPos();
                final ResourceLocation resource = resourceName;
                final float vol = volume;
                final float soundPitch = pitch;

                context.issueMainThreadTask(new ILuaTask()
                {
                    @Nullable
                    @Override
                    public Object[] execute()
                    {
                        world.playSound( null, pos, SoundEvent.REGISTRY.getObject( resource ), SoundCategory.RECORDS, Math.min( vol, 3f ), soundPitch );
                        return null;
                    }
                });

                m_lastPlayTime = m_clock;
                return new Object[]{true}; // Success, return true
            }
            else
            {
                return new Object[]{false}; // Failed - sound not existent, return false
            }
        }
        else
        {
            return new Object[]{false}; // Failed - rate limited, return false
        }
    }
}

