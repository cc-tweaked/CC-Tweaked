/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.peripheral.common.TilePeripheralBase;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nonnull;

public abstract class TileModemBase extends TilePeripheralBase
{
    private static final AxisAlignedBB[] BOXES = new AxisAlignedBB[] {
        new AxisAlignedBB( 0.125, 0.0, 0.125, 0.875, 0.1875, 0.875 ), // Down
        new AxisAlignedBB( 0.125, 0.8125, 0.125, 0.875, 1.0, 0.875 ), // Up
        new AxisAlignedBB( 0.125, 0.125, 0.0, 0.875, 0.875, 0.1875 ), // North
        new AxisAlignedBB( 0.125, 0.125, 0.8125, 0.875, 0.875, 1.0 ), // South
        new AxisAlignedBB( 0.0, 0.125, 0.125, 0.1875, 0.875, 0.875 ), // West
        new AxisAlignedBB( 0.8125, 0.125, 0.125, 1.0, 0.875, 0.875 ), // East
    };

    protected ModemPeripheral m_modem;

    protected TileModemBase()
    {
        m_modem = createPeripheral();
    }

    protected abstract ModemPeripheral createPeripheral();

    @Override
    public void destroy()
    {
        if( m_modem != null )
        {
            m_modem.destroy();
            m_modem = null;
        }
    }

    @Override
    public boolean isSolidOnSide( int side )
    {
        return false;
    }

    @Override
    public void onNeighbourChange()
    {
        EnumFacing dir = getDirection();
        if( !getWorld().isSideSolid( getPos().offset( dir ), dir.getOpposite() ) )
        {
            // Drop everything and remove block
            ((BlockGeneric) getBlockType()).dropAllItems( getWorld(), getPos(), false );
            getWorld().setBlockToAir( getPos() );
        }
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBounds()
    {
        int direction = getDirection().ordinal();
        return direction >= 0 && direction < BOXES.length ? BOXES[direction] : Block.FULL_BLOCK_AABB;
    }

    @Override
    public void update()
    {
        super.update();
        if( !getWorld().isRemote && m_modem.getModemState().pollChanged() )
        {
            updateAnim();
        }
    }

    protected void updateAnim()
    {
        setAnim( m_modem.getModemState().isOpen() ? 1 : 0 );
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.readDescription( nbttagcompound );
        updateBlock();
    }

    // IPeripheralTile implementation

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return side == getDirection() ? m_modem : null;
    }
}
