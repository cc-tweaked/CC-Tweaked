/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class TileSpeaker extends TileGeneric implements Tickable
{
    public static final int MIN_TICKS_BETWEEN_SOUNDS = 1;

    private final SpeakerPeripheral peripheral;
    private LazyOptional<IPeripheral> peripheralCap;

    public TileSpeaker( BlockEntityType<TileSpeaker> type )
    {
        super( type );
        peripheral = new Peripheral( this );
    }

    @Override
    public void tick()
    {
        peripheral.update();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable Direction side )
    {
        if( cap == CAPABILITY_PERIPHERAL )
        {
            if( peripheralCap == null ) peripheralCap = LazyOptional.of( () -> peripheral );
            return peripheralCap.cast();
        }

        return super.getCapability( cap, side );
    }

    @Override
    protected void invalidateCaps()
    {
        super.invalidateCaps();
        peripheralCap = CapabilityUtil.invalidate( peripheralCap );
    }

    private static final class Peripheral extends SpeakerPeripheral
    {
        private final TileSpeaker speaker;

        private Peripheral( TileSpeaker speaker )
        {
            this.speaker = speaker;
        }

        @Override
        public World getWorld()
        {
            return speaker.getWorld();
        }

        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = speaker.getPos();
            return new Vec3d( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Override
        public boolean equals( @Nullable IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && speaker == ((Peripheral) other).speaker);
        }
    }
}
