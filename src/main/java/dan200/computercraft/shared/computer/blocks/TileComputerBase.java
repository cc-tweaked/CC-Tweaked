/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.IDirectionalTile;
import dan200.computercraft.shared.common.ITerminal;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.PeripheralUtil;
import dan200.computercraft.shared.util.RedstoneUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public abstract class TileComputerBase extends TileGeneric
    implements IComputerTile, IDirectionalTile, ITickable
{
    protected int m_instanceID;
    protected int m_computerID;
    protected String m_label;
    protected boolean m_on;
    protected boolean m_startOn;
    protected boolean m_fresh;

    protected TileComputerBase()
    {
        m_instanceID = -1;
        m_computerID = -1;
        m_label = null;
        m_on = false;
        m_startOn = false;
        m_fresh = false;
    }

    @Override
    public BlockComputerBase getBlock()
    {
        Block block = super.getBlock();
        if( block instanceof BlockComputerBase )
        {
            return (BlockComputerBase) block;
        }
        return null;
    }

    protected void unload()
    {
        if( m_instanceID >= 0 )
        {
            if( !getWorld().isRemote )
            {
                ComputerCraft.serverComputerRegistry.remove( m_instanceID );
            }
            m_instanceID = -1;
        }
    }

    @Override
    public void destroy()
    {
        unload();
        for( EnumFacing dir : EnumFacing.VALUES )
        {
            RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
        }
    }

    @Override
    public void onChunkUnload()
    {
        unload();
    }

    @Override
    public void invalidate()
    {
        unload();
        super.invalidate();
    }

    public abstract void openGUI( EntityPlayer player );

    protected boolean canNameWithTag( EntityPlayer player )
    {
        return false;
    }

    protected boolean onDefaultComputerInteract( EntityPlayer player )
    {
        if( !getWorld().isRemote )
        {
            if( isUsable( player, false ) )
            {
                createServerComputer().turnOn();
                openGUI( player );
            }
        }
        return true;
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        ItemStack currentItem = player.getHeldItem( EnumHand.MAIN_HAND );
        if( !currentItem.isEmpty() && currentItem.getItem() == Items.NAME_TAG && canNameWithTag( player ) )
        {
            // Label to rename computer
            if( !getWorld().isRemote )
            {
                if( currentItem.hasDisplayName() )
                {
                    setLabel( currentItem.getDisplayName() );
                }
                else
                {
                    setLabel( null );
                }
                currentItem.shrink( 1 );
            }
            return true;
        }
        else if( !player.isSneaking() )
        {
            // Regular right click to activate computer
            return onDefaultComputerInteract( player );
        }
        return false;
    }

    public int getRedstoneOutput( @Nonnull EnumFacing side )
    {
        int localDir = remapLocalSide( DirectionUtil.toLocal( this, side ) );
        if( !getWorld().isRemote )
        {
            ServerComputer computer = getServerComputer();
            if( computer != null ) return computer.getRedstoneOutput( localDir );
        }
        return 0;
    }

    public int getBundledRedstoneOutput( @Nonnull EnumFacing side )
    {
        int localDir = remapLocalSide( DirectionUtil.toLocal( this, side ) );
        if( !getWorld().isRemote )
        {
            ServerComputer computer = getServerComputer();
            if( computer != null ) return computer.getBundledRedstoneOutput( localDir );
        }
        return 0;
    }

    @Override
    public void onNeighbourChange()
    {
        updateInput();
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        updateInput( neighbour );
    }

    @Override
    public void update()
    {
        if( !getWorld().isRemote )
        {
            ServerComputer computer = createServerComputer();
            if( computer != null )
            {
                if( m_startOn || (m_fresh && m_on) )
                {
                    computer.turnOn();
                    m_startOn = false;
                }
                computer.keepAlive();
                if( computer.hasOutputChanged() )
                {
                    updateOutput();
                }
                m_fresh = false;
                m_computerID = computer.getID();
                m_label = computer.getLabel();
                m_on = computer.isOn();
            }
        }
        else
        {
            ClientComputer computer = createClientComputer();
            if( computer != null )
            {
                if( computer.hasOutputChanged() )
                {
                    updateBlock();
                }
            }
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound nbttagcompound )
    {
        nbttagcompound = super.writeToNBT( nbttagcompound );

        // Save ID, label and power state
        if( m_computerID >= 0 )
        {
            nbttagcompound.setInteger( "computerID", m_computerID );
        }
        if( m_label != null )
        {
            nbttagcompound.setString( "label", m_label );
        }
        nbttagcompound.setBoolean( "on", m_on );
        return nbttagcompound;
    }

    @Override
    public void readFromNBT( NBTTagCompound nbttagcompound )
    {
        super.readFromNBT( nbttagcompound );

        // Load ID
        int id = -1;
        if( nbttagcompound.hasKey( "computerID" ) )
        {
            // Post-1.6 computers
            id = nbttagcompound.getInteger( "computerID" );
        }
        else if( nbttagcompound.hasKey( "userDir" ) )
        {
            // Pre-1.6 computers
            String userDir = nbttagcompound.getString( "userDir" );
            try
            {
                id = Integer.parseInt( userDir );
            }
            catch( NumberFormatException e )
            {
                // Ignore badly formatted data
            }
        }
        m_computerID = id;

        // Load label
        if( nbttagcompound.hasKey( "label" ) )
        {
            m_label = nbttagcompound.getString( "label" );
        }
        else
        {
            m_label = null;
        }

        // Load power state
        m_startOn = nbttagcompound.getBoolean( "on" );
        m_on = m_startOn;
    }

    protected boolean isPeripheralBlockedOnSide( int localSide )
    {
        return false;
    }

    protected int remapLocalSide( int localSide )
    {
        return localSide;
    }

    private void updateSideInput( ServerComputer computer, EnumFacing dir, BlockPos offset )
    {
        EnumFacing offsetSide = dir.getOpposite();
        int localDir = remapLocalSide( DirectionUtil.toLocal( this, dir ) );

        computer.setRedstoneInput( localDir, RedstoneUtil.getRedstoneOutput( getWorld(), offset, offsetSide ) );
        computer.setBundledRedstoneInput( localDir, RedstoneUtil.getBundledRedstoneOutput( getWorld(), offset, offsetSide ) );

        if( !isPeripheralBlockedOnSide( localDir ) )
        {
            computer.setPeripheral( localDir, PeripheralUtil.getPeripheral( getWorld(), offset, offsetSide ) );
        }
    }

    public void updateInput()
    {
        if( getWorld() == null || getWorld().isRemote )
        {
            return;
        }

        // Update redstone and peripherals
        ServerComputer computer = getServerComputer();
        if( computer != null )
        {
            BlockPos pos = computer.getPosition();
            for( EnumFacing dir : EnumFacing.VALUES )
            {
                updateSideInput( computer, dir, pos.offset( dir ) );
            }
        }
    }

    public void updateInput( BlockPos neighbour )
    {
        if( getWorld() == null || getWorld().isRemote )
        {
            return;
        }

        ServerComputer computer = getServerComputer();
        if( computer != null )
        {
            BlockPos pos = computer.getPosition();
            for( EnumFacing dir : EnumFacing.VALUES )
            {
                BlockPos offset = pos.offset( dir );
                if( offset.equals( neighbour ) )
                {
                    updateSideInput( computer, dir, offset );
                    break;
                }
            }
        }
    }

    public void updateOutput()
    {
        // Update redstone
        updateBlock();
        for( EnumFacing dir : EnumFacing.VALUES )
        {
            RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
        }
    }

    protected abstract ServerComputer createComputer( int instanceID, int id );

    public abstract ComputerProxy createProxy();

    // ITerminalTile

    @Override
    public ITerminal getTerminal()
    {
        return getComputer();
    }

    // IComputerTile

    @Override
    public void setComputerID( int id )
    {
        if( !getWorld().isRemote && m_computerID != id )
        {
            m_computerID = id;
            ServerComputer computer = getServerComputer();
            if( computer != null )
            {
                computer.setID( m_computerID );
            }
            markDirty();
        }
    }

    @Override
    public void setLabel( String label )
    {
        if( !getWorld().isRemote )
        {
            createServerComputer().setLabel( label );
        }
    }

    @Override
    public IComputer createComputer()
    {
        if( getWorld().isRemote )
        {
            return createClientComputer();
        }
        else
        {
            return createServerComputer();
        }
    }

    @Override
    public IComputer getComputer()
    {
        if( getWorld().isRemote )
        {
            return getClientComputer();
        }
        else
        {
            return getServerComputer();
        }
    }

    @Override
    public ComputerFamily getFamily()
    {
        BlockComputerBase block = getBlock();
        return block != null ? block.getFamily() : ComputerFamily.Normal;
    }

    public ServerComputer createServerComputer()
    {
        if( !getWorld().isRemote )
        {
            boolean changed = false;
            if( m_instanceID < 0 )
            {
                m_instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
                changed = true;
            }
            if( !ComputerCraft.serverComputerRegistry.contains( m_instanceID ) )
            {
                ServerComputer computer = createComputer( m_instanceID, m_computerID );
                ComputerCraft.serverComputerRegistry.add( m_instanceID, computer );
                m_fresh = true;
                changed = true;
            }
            if( changed )
            {
                updateBlock();
                updateInput();
            }
            return ComputerCraft.serverComputerRegistry.get( m_instanceID );
        }
        return null;
    }

    public ServerComputer getServerComputer()
    {
        if( !getWorld().isRemote )
        {
            return ComputerCraft.serverComputerRegistry.get( m_instanceID );
        }
        return null;
    }

    public ClientComputer createClientComputer()
    {
        if( getWorld().isRemote )
        {
            if( m_instanceID >= 0 )
            {
                if( !ComputerCraft.clientComputerRegistry.contains( m_instanceID ) )
                {
                    ComputerCraft.clientComputerRegistry.add( m_instanceID, new ClientComputer( m_instanceID ) );
                }
                return ComputerCraft.clientComputerRegistry.get( m_instanceID );
            }
        }
        return null;
    }

    public ClientComputer getClientComputer()
    {
        if( getWorld().isRemote )
        {
            return ComputerCraft.clientComputerRegistry.get( m_instanceID );
        }
        return null;
    }

    // Networking stuff

    @Override
    public void writeDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.writeDescription( nbttagcompound );
        nbttagcompound.setInteger( "instanceID", createServerComputer().getInstanceID() );
    }

    @Override
    public void readDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.readDescription( nbttagcompound );
        m_instanceID = nbttagcompound.getInteger( "instanceID" );
    }

    protected void transferStateFrom( TileComputerBase copy )
    {
        if( copy.m_computerID != m_computerID || copy.m_instanceID != m_instanceID )
        {
            unload();
            m_instanceID = copy.m_instanceID;
            m_computerID = copy.m_computerID;
            m_label = copy.m_label;
            m_on = copy.m_on;
            m_startOn = copy.m_startOn;
            updateBlock();
        }
        copy.m_instanceID = -1;
    }
}
