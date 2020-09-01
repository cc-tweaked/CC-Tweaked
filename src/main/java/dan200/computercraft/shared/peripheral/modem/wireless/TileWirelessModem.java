/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileWirelessModem extends TileGeneric implements IPeripheralTile
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
        public World getWorld()
        {
            return entity.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = entity.getPos().offset( entity.modemDirection );
            return new Vec3d( pos.getX(), pos.getY(), pos.getZ() );
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

    private boolean hasModemDirection = false;
    private Direction modemDirection = Direction.DOWN;
    private final ModemPeripheral modem;
    private boolean destroyed = false;

    public TileWirelessModem( BlockEntityType<? extends TileWirelessModem> type, boolean advanced )
    {
        super( type );
        this.advanced = advanced;
        modem = new Peripheral( this );
    }

    @Override
    public void cancelRemoval()
    {
        super.cancelRemoval();
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
    public void resetBlock()
    {
        super.resetBlock();
        hasModemDirection = false;
        world.getBlockTickScheduler().schedule( getPos(), getCachedState().getBlock(), 0 );
    }

    @Override
    public void blockTick()
    {
        Direction currentDirection = modemDirection;
        refreshDirection();
        // Invalidate the capability if the direction has changed. I'm not 100% happy with this implementation
        //  - ideally we'd do it within refreshDirection or updateContainingBlockInfo, but this seems the _safest_
        //  place.
        if( modem.getModemState().pollChanged() ) updateBlockState();
    }

    private void refreshDirection()
    {
        if( hasModemDirection ) return;

        hasModemDirection = true;
        modemDirection = getCachedState().get( BlockWirelessModem.FACING );
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral(Direction side) {
        this.refreshDirection();
        return side == this.modemDirection ? this.modem : null;
    }

    private void updateBlockState()
    {
        boolean on = modem.getModemState().isOpen();
        BlockState state = getCachedState();
        if( state.get( BlockWirelessModem.ON ) != on )
        {
            getWorld().setBlockState( getPos(), state.with( BlockWirelessModem.ON, on ) );
        }
    }
}
