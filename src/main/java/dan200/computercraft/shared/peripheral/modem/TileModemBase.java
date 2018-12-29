/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.peripheral.common.TilePeripheralBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public abstract class TileModemBase extends TilePeripheralBase
{

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
    public final void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        updateBlock();
    }

    // IPeripheralTile implementation

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return side == getDirection() ? m_modem : null;
    }
}
