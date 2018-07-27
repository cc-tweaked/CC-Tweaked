/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;

public class TileComputer extends TileComputerBase
{
    // Statics

    // Members
    private ComputerProxy m_proxy;
    
    public TileComputer()
    {
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ComputerFamily family = getFamily();
        ServerComputer computer = new ServerComputer(
            getWorld(),
            id,
            m_label,
            instanceID,
            family,
            ComputerCraft.terminalWidth_computer,
            ComputerCraft.terminalHeight_computer
        );
        computer.setPosition( getPos() );
        return computer;
    }

    @Override
    public ComputerProxy createProxy()
    {
        if( m_proxy == null )
        {
            m_proxy = new ComputerProxy()
            {
                @Override
                protected TileComputerBase getTile()
                {
                    return TileComputer.this;
                }
            };
        }
        return m_proxy;
    }

    @Override
    public void getDroppedItems( @Nonnull NonNullList<ItemStack> drops, boolean creative )
    {
        IComputer computer = getComputer();
        if( !creative || (computer != null && computer.getLabel() != null) )
        {
            drops.add( ComputerItemFactory.create( this ) );
        }
    }

    @Override
    public ItemStack getPickedItem()
    {
        return ComputerItemFactory.create( this );
    }

    @Override
    public void openGUI( EntityPlayer player )
    {
        ComputerCraft.openComputerGUI( player, this );
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.readDescription( nbttagcompound );
        updateBlock();
    }

    public boolean isUseableByPlayer( EntityPlayer player )
    {
        return isUsable( player, false );
    }

    // IDirectionalTile

    @Override
    public EnumFacing getDirection()
    {
        IBlockState state = getBlockState();
        return state.getValue( BlockComputer.FACING );
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
        if( dir.getAxis() == EnumFacing.Axis.Y )
        {
            dir = EnumFacing.NORTH;
        }
        setBlockState( getBlockState().withProperty( BlockComputer.FACING, dir ) );
        updateInput();
    }

    // For legacy reasons, computers invert the meaning of "left" and "right"
    private static final int[] s_remapSide = { 0, 1, 2, 3, 5, 4 };

    @Override
    protected int remapLocalSide( int localSide )
    {
        return s_remapSide[ localSide ];
    }
}
