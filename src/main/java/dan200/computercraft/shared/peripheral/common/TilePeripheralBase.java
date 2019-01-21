/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.IDirectionalTile;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;

public abstract class TilePeripheralBase extends TileGeneric implements IPeripheralTile, ITickable, IDirectionalTile, ITilePeripheral
{
    private EnumFacing m_dir;
    private int m_anim;
    private boolean m_changed;

    private String m_label;

    public TilePeripheralBase()
    {
        m_dir = EnumFacing.NORTH;
        m_anim = 0;
        m_changed = false;

        m_label = null;
    }


    @Override
    public BlockPeripheral getBlock()
    {
        return (BlockPeripheral) super.getBlock();
    }

    @Override
    public void getDroppedItems( @Nonnull NonNullList<ItemStack> drops, boolean creative )
    {
        if( !creative )
        {
            drops.add( PeripheralItemFactory.create( this ) );
        }
    }

    @Override
    public final PeripheralType getPeripheralType()
    {
        return getBlock().getPeripheralType( getBlockState() );
    }

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return null;
    }

    @Override
    public String getLabel()
    {
        if( m_label != null && m_label.length() > 0 )
        {
            return m_label;
        }
        return null;
    }

    @Override
    public void setLabel( String label )
    {
        m_label = label;
    }

    // IDirectionalTile implementation

    @Override
    public EnumFacing getDirection()
    {
        return m_dir;
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
        if( dir != m_dir )
        {
            m_dir = dir;
            m_changed = true;
        }
    }

    public int getAnim()
    {
        return m_anim;
    }

    protected void setAnim( int anim )
    {
        if( anim != m_anim )
        {
            m_anim = anim;
            m_changed = true;
        }
    }

    @Override
    public void update()
    {
        if( m_changed )
        {
            m_changed = false;
            updateBlock();
        }
    }

    @Override
    public void readFromNBT( NBTTagCompound nbt )
    {
        // Read properties
        super.readFromNBT( nbt );
        if( nbt.hasKey( "dir" ) )
        {
            m_dir = EnumFacing.byIndex( nbt.getInteger( "dir" ) );
        }
        if( nbt.hasKey( "anim" ) )
        {
            m_anim = nbt.getInteger( "anim" );
        }
        if( nbt.hasKey( "label" ) )
        {
            m_label = nbt.getString( "label" );
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound nbt )
    {
        // Write properties
        nbt = super.writeToNBT( nbt );
        nbt.setInteger( "dir", m_dir.getIndex() );
        nbt.setInteger( "anim", m_anim );
        if( m_label != null ) nbt.setString( "label", m_label );
        return nbt;
    }

    @Override
    public void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        m_dir = EnumFacing.byIndex( nbt.getInteger( "dir" ) );
        m_anim = nbt.getInteger( "anim" );
        if( nbt.hasKey( "label" ) )
        {
            m_label = nbt.getString( "label" );
        }
        else
        {
            m_label = null;
        }
    }

    @Override
    public void writeDescription( @Nonnull NBTTagCompound nbt )
    {
        super.writeDescription( nbt );
        nbt.setInteger( "dir", m_dir.getIndex() );
        nbt.setInteger( "anim", m_anim );
        if( m_label != null )
        {
            nbt.setString( "label", m_label );
        }
    }
}
