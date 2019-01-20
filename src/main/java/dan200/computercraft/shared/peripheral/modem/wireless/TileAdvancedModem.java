/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.common.IPeripheralTile;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TileAdvancedModem extends TileGeneric implements IPeripheralTile
{
    private static class Peripheral extends WirelessModemPeripheral
    {
        private TileAdvancedModem tile;

        Peripheral( TileAdvancedModem entity )
        {
            super( new ModemState( () -> TickScheduler.schedule( entity ) ), true );
            tile = entity;
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            return tile.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = tile.getPos().offset( tile.modemDirection );
            return new Vec3d( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other;
        }
    }

    private boolean hasModemDirection = false;
    private EnumFacing modemDirection = EnumFacing.DOWN;
    private final ModemPeripheral modem = new Peripheral( this );
    private boolean destroyed = false;

    private boolean on = false;

    @Override
    public void onLoad()
    {
        super.onLoad();
        updateDirection();
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
    public void updateContainingBlockInfo()
    {
        hasModemDirection = false;
        super.updateContainingBlockInfo();
        world.scheduleUpdate( getPos(), ComputerCraft.Blocks.advancedModem, 0 );
    }

    @Override
    public void updateTick()
    {
        updateDirection();

        if( modem.getModemState().pollChanged() )
        {
            boolean newOn = modem.getModemState().isOpen();
            if( newOn != on )
            {
                on = newOn;
                updateBlock();
            }
        }
    }

    private void updateDirection()
    {
        if( !hasModemDirection )
        {
            hasModemDirection = true;
            modemDirection = getDirection();
        }
    }


    private EnumFacing getDirection()
    {
        return getBlockState().getValue( BlockAdvancedModem.Properties.FACING );
    }

    @Override
    protected void writeDescription( @Nonnull NBTTagCompound nbt )
    {
        super.writeDescription( nbt );
        nbt.setBoolean( "on", on );
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        on = nbt.getBoolean( "on" );
        updateBlock();
    }

    public boolean isOn()
    {
        return on;
    }

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return !destroyed && side == getDirection() ? modem : null;
    }
}
