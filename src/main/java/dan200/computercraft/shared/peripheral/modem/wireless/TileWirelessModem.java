/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.common.IPeripheralTile;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class TileWirelessModem extends TileGeneric implements IPeripheralTile, ITickable
{
    private static class Peripheral extends WirelessModemPeripheral
    {
        private TileWirelessModem tile;

        public Peripheral( TileWirelessModem entity, boolean advanced )
        {
            super( advanced );
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
            BlockPos pos = tile.getPos().offset( tile.getCachedSide() );
            return new Vec3d( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            if( other instanceof Peripheral )
            {
                Peripheral otherModem = (Peripheral) other;
                return otherModem.tile == tile;
            }
            return false;
        }
    }

    private EnumFacing cachedSide = null;
    private ModemPeripheral modem;
    private boolean modemOn = false;

    public TileWirelessModem( boolean isAdvanced )
    {
        modem = new Peripheral( this, isAdvanced );
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        fetchBlockInfo();
    }

    @Override
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        cachedSide = null;
    }

    protected EnumFacing getSide()
    {
        if( cachedSide == null ) fetchBlockInfo();
        return cachedSide;
    }

    private EnumFacing getCachedSide()
    {
        return cachedSide == null ? EnumFacing.NORTH : cachedSide;
    }

    private void fetchBlockInfo()
    {
        IBlockState state = getBlockState();
        blockType = state.getBlock();
        cachedSide = state.getValue( BlockWirelessModem.FACING );
    }

    @Override
    public synchronized void destroy()
    {
        if( modem != null )
        {
            modem.destroy();
            modem = null;
        }
    }

    @Override
    public void onNeighbourChange()
    {
        EnumFacing side = getSide();
        if( !getWorld().isSideSolid( getPos().offset( side ), side.getOpposite() ) )
        {
            // Drop everything and remove block
            getBlock().dropBlockAsItem( getWorld(), getPos(), getBlockState(), 1 );
            getWorld().setBlockToAir( getPos() );
        }
    }

    @Override
    public void update()
    {
        if( cachedSide == null ) fetchBlockInfo();
        if( !getWorld().isRemote && modem.pollChanged() ) updateVisualState();
    }

    private void updateVisualState()
    {
        boolean modemOn = modem != null && modem.isActive();
        if( modemOn != this.modemOn )
        {
            this.modemOn = modemOn;
            updateBlock();
        }
    }

    @Override
    protected void writeDescription( @Nonnull NBTTagCompound tag )
    {
        super.writeDescription( tag );
        tag.setBoolean( "modem_on", modemOn );
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound tag )
    {
        super.readDescription( tag );
        modemOn = tag.getBoolean( "modem_on" );

        updateBlock();
    }

    public boolean isModemOn()
    {
        return modemOn;
    }

    protected boolean isAttached()
    {
        return modem != null && modem.getComputer() != null;
    }

    // IPeripheralTile implementation

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return side == getSide() ? modem : null;
    }

    public static class TileNormalWirelessModem extends TileWirelessModem
    {
        public TileNormalWirelessModem()
        {
            super( false );
        }
    }

    public static class TileAdvancedWirelessModem extends TileWirelessModem
    {
        public TileAdvancedWirelessModem()
        {
            super( true );
        }
    }
}
