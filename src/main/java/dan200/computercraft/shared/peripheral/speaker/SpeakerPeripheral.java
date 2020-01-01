/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

import static dan200.computercraft.api.lua.ArgumentHelper.getString;
import static dan200.computercraft.api.lua.ArgumentHelper.optFiniteDouble;

public abstract class SpeakerPeripheral implements IPeripheral
{
    private long m_clock = 0;
    private long m_lastPlayTime = 0;
    private final AtomicInteger m_notesThisTick = new AtomicInteger();

    public void update()
    {
        m_clock++;
        m_notesThisTick.set( 0 );
    }

    public abstract World getWorld();

    public Vec3d getPosition()
    {
        // FIXME: Should be abstract, but we need this for Plethora compat. We'll
        //  be able to change this in a few versions as we implement both there.
        BlockPos pos = getPos();
        return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
    }

    @Deprecated
    public BlockPos getPos()
    {
        return new BlockPos( getPosition() );
    }

    public boolean madeSound( long ticks )
    {
        return m_clock - m_lastPlayTime <= ticks;
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
            "playSound",
            "playNote",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computerAccess, @Nonnull ILuaContext context, int methodIndex, @Nonnull Object[] args ) throws LuaException
    {
        switch( methodIndex )
        {
            case 0: // playSound
            {
                String name = getString( args, 0 );
                float volume = (float) optFiniteDouble( args, 1, 1.0 );
                float pitch = (float) optFiniteDouble( args, 2, 1.0 );

                return new Object[] { playSound( context, name, volume, pitch, false ) };
            }

            case 1: // playNote
                return playNote( args, context );

            default:
                throw new IllegalStateException( "Method index out of range!" );
        }
    }

    @Nonnull
    private synchronized Object[] playNote( Object[] arguments, ILuaContext context ) throws LuaException
    {
        String name = getString( arguments, 0 );
        float volume = (float) optFiniteDouble( arguments, 1, 1.0 );
        float pitch = (float) optFiniteDouble( arguments, 2, 1.0 );

        String noteName = "block.note." + name;

        // Check if the note exists
        if( !SoundEvent.REGISTRY.containsKey( new ResourceLocation( noteName ) ) )
        {
            throw new LuaException( "Invalid instrument, \"" + name + "\"!" );
        }

        // If the resource location for note block notes changes, this method call will need to be updated
        boolean success = playSound( context, noteName, volume, (float) Math.pow( 2.0, (pitch - 12.0) / 12.0 ), true );

        if( success ) m_notesThisTick.incrementAndGet();
        return new Object[] { success };
    }

    private synchronized boolean playSound( ILuaContext context, String name, float volume, float pitch, boolean isNote ) throws LuaException
    {
        if( m_clock - m_lastPlayTime < TileSpeaker.MIN_TICKS_BETWEEN_SOUNDS &&
            (!isNote || m_clock - m_lastPlayTime != 0 || m_notesThisTick.get() >= ComputerCraft.maxNotesPerTick) )
        {
            // Rate limiting occurs when we've already played a sound within the last tick, or we've
            // played more notes than allowable within the current tick.
            return false;
        }

        World world = getWorld();
        Vec3d pos = getPosition();

        context.issueMainThreadTask( () -> {
            MinecraftServer server = world.getMinecraftServer();
            if( server == null ) return null;

            float adjVolume = Math.min( volume, 3.0f );
            server.getPlayerList().sendToAllNearExcept(
                null, pos.x, pos.y, pos.z, adjVolume > 1.0f ? 16 * adjVolume : 16.0, world.provider.getDimension(),
                new SPacketCustomSound( name, SoundCategory.RECORDS, pos.x, pos.y, pos.z, adjVolume, pitch )
            );
            return null;
        } );

        m_lastPlayTime = m_clock;
        return true;
    }
}

