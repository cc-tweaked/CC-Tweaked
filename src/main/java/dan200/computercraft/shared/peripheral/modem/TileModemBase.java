/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import dan200.computercraft.api.peripheral.IPeripheral;
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
    public synchronized void destroy()
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
        if( !getWorld().isSideSolid(
            getPos().offset( dir ),
            dir.getOpposite()
        ) )
        {
            // Drop everything and remove block
            getBlock().dropBlockAsItem( getWorld(), getPos(), getBlockState(), 1 );
            getWorld().setBlockToAir( getPos() );
        }
    }

    @Override
    public void update()
    {
        super.update();
        if( !getWorld().isRemote && m_modem.pollChanged() )
        {
            updateAnim();
        }
    }

    protected void updateAnim()
    {
        if( m_modem.isActive() )
        {
            setAnim( 1 );
        }
        else
        {
            setAnim( 0 );
        }
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
        if( side == getDirection() )
        {
            return m_modem;
        }
        return null;
    }

    protected boolean isAttached()
    {
        return (m_modem != null) && (m_modem.getComputer() != null);
    }
}
