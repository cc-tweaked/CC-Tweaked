/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class TileWirelessModem extends TileGeneric implements IPeripheralTile
{
    private final boolean advanced;
    private final ModemPeripheral modem;
    private boolean hasModemDirection = false;
    private Direction modemDirection = Direction.DOWN;
    private boolean destroyed = false;

    public TileWirelessModem( BlockEntityType<? extends TileWirelessModem> type, boolean advanced, BlockPos pos, BlockState state )
    {
        super( type, pos, state );
        this.advanced = advanced;
        modem = new Peripheral( this );
    }

    @Override
    public void clearRemoved()
    {
        super.clearRemoved();
        TickScheduler.schedule( this );
    }

    @Override
    public void setBlockState( BlockState state )
    {
        super.setBlockState( state );
        if( state != null ) return;
        hasModemDirection = false;
        level.scheduleTick( getBlockPos(), getBlockState().getBlock(), 0 );
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
    public void blockTick()
    {
        Direction currentDirection = modemDirection;
        refreshDirection();
        // Invalidate the capability if the direction has changed. I'm not 100% happy with this implementation
        //  - ideally we'd do it within refreshDirection or updateContainingBlockInfo, but this seems the _safest_
        //  place.
        if( modem.getModemState()
            .pollChanged() )
        {
            updateBlockState();
        }
    }

    private void refreshDirection()
    {
        if( hasModemDirection )
        {
            return;
        }

        hasModemDirection = true;
        modemDirection = getBlockState().getValue( BlockWirelessModem.FACING );
    }

    private void updateBlockState()
    {
        boolean on = modem.getModemState()
            .isOpen();
        BlockState state = getBlockState();
        if( state.getValue( BlockWirelessModem.ON ) != on )
        {
            getLevel().setBlockAndUpdate( getBlockPos(), state.setValue( BlockWirelessModem.ON, on ) );
        }
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        refreshDirection();
        return side == modemDirection ? modem : null;
    }

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
            BlockPos pos = entity.getBlockPos()
                .relative( entity.modemDirection );
            return new Vec3( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Nonnull
        @Override
        public Object getTarget()
        {
            return entity;
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && entity == ((Peripheral) other).entity);
        }
    }
}
