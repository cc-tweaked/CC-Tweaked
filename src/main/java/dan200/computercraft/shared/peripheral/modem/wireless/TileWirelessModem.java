/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.CapabilityUtil;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class TileWirelessModem extends TileGeneric
{
    private static class Peripheral extends WirelessModemPeripheral
    {
        private final TileWirelessModem entity;

        Peripheral( TileWirelessModem entity )
        {
            super( new ModemState( () -> TickScheduler.schedule( entity ) ), entity.advanced );
            this.entity = entity;
        }

        @Nonnull
        @Override
        public Level getLevel()
        {
            return entity.getLevel();
        }

        @Nonnull
        @Override
        public Vec3 getPosition()
        {
            BlockPos pos = entity.getBlockPos().relative( entity.getDirection() );
            return new Vec3( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && entity == ((Peripheral) other).entity);
        }

        @Nonnull
        @Override
        public Object getTarget()
        {
            return entity;
        }
    }

    private final boolean advanced;

    private final ModemPeripheral modem;
    private boolean destroyed = false;
    private LazyOptional<IPeripheral> modemCap;

    public TileWirelessModem( BlockEntityType<? extends TileWirelessModem> type, BlockPos pos, BlockState state, boolean advanced )
    {
        super( type, pos, state );
        this.advanced = advanced;
        modem = new Peripheral( this );
    }

    @Override
    public void clearRemoved()
    {
        super.clearRemoved(); // TODO: Replace with onLoad
        TickScheduler.schedule( this );
    }

    @Override
    public void destroy()
    {
        if( !destroyed )
        {
            modem.destroy();
            destroyed = true;
        }
    }

    @Override
    @Deprecated
    public void setBlockState( @Nonnull BlockState state )
    {
        Direction direction = getDirection();
        super.setBlockState( state );
        if( getDirection() != direction ) modemCap = CapabilityUtil.invalidate( modemCap );
    }

    @Override
    public void blockTick()
    {
        if( modem.getModemState().pollChanged() ) updateBlockState();
    }

    @Nonnull
    private Direction getDirection()
    {
        return getBlockState().getValue( BlockWirelessModem.FACING );
    }

    private void updateBlockState()
    {
        boolean on = modem.getModemState().isOpen();
        BlockState state = getBlockState();
        if( state.getValue( BlockWirelessModem.ON ) != on )
        {
            getLevel().setBlockAndUpdate( getBlockPos(), state.setValue( BlockWirelessModem.ON, on ) );
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable Direction side )
    {
        if( cap == CAPABILITY_PERIPHERAL )
        {
            if( side != null && getDirection() != side ) return LazyOptional.empty();
            if( modemCap == null ) modemCap = LazyOptional.of( () -> modem );
            return modemCap.cast();
        }

        return super.getCapability( cap, side );
    }
}
