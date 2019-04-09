/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.Containers;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public class TileComputer extends TileComputerBase
{
    private static final String TAG_STATE = "state";

    private ComputerProxy m_proxy;
    private ComputerState state = ComputerState.Off;

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
    public void openGUI( EntityPlayer player )
    {
        Containers.openComputerGUI( player, this );
    }

    @Override
    protected void writeDescription( @Nonnull NBTTagCompound nbt )
    {
        super.writeDescription( nbt );
        nbt.setInteger( TAG_STATE, state.ordinal() );
    }

    @Override
    protected final void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        state = ComputerState.valueOf( nbt.getInteger( TAG_STATE ) );
        updateBlock();
    }

    public boolean isUsableByPlayer( EntityPlayer player )
    {
        return isUsable( player, false );
    }

    @Override
    public void update()
    {
        super.update();
        if( !world.isRemote )
        {
            ServerComputer computer = getServerComputer();
            state = computer == null ? ComputerState.Off : computer.getState();
        }
    }

    // IDirectionalTile

    @Override
    public EnumFacing getDirection()
    {
        IBlockState state = getBlockState();
        return state.getValue( BlockComputer.Properties.FACING );
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
        if( dir.getAxis() == EnumFacing.Axis.Y ) dir = EnumFacing.NORTH;
        setBlockState( getBlockState().withProperty( BlockComputer.Properties.FACING, dir ) );
        updateInput();
    }

    @Override
    protected ComputerSide remapLocalSide( ComputerSide localSide )
    {
        // For legacy reasons, computers invert the meaning of "left" and "right". A computer's front is facing
        // towards you, but a turtle's front is facing the other way.
        if( localSide == ComputerSide.RIGHT ) return ComputerSide.LEFT;
        if( localSide == ComputerSide.LEFT ) return ComputerSide.RIGHT;
        return localSide;
    }

    public ComputerState getState()
    {
        return state;
    }
}
