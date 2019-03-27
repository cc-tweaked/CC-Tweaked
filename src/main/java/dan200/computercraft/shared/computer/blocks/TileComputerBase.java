/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.BundledRedstone;
import dan200.computercraft.shared.Peripherals;
import dan200.computercraft.shared.common.IDirectionalTile;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.util.DirectionUtil;
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
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class TileComputerBase extends TileGeneric implements IComputerTile, IDirectionalTile, ITickable, IPeripheralTile
{
    private int m_instanceID = -1;
    private int m_computerID = -1;
    protected String m_label = null;
    private boolean m_on = false;
    boolean m_startOn = false;
    private boolean m_fresh = false;

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
    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        ItemStack currentItem = player.getHeldItem( hand );
        if( !currentItem.isEmpty() && currentItem.getItem() == Items.NAME_TAG && canNameWithTag( player ) )
        {
            // Label to rename computer
            if( !getWorld().isRemote )
            {
                setLabel( currentItem.hasDisplayName() ? currentItem.getDisplayName() : null );
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

    @Override
    public boolean getRedstoneConnectivity( EnumFacing side )
    {
        if( side == null ) return false;
        int localDir = remapLocalSide( DirectionUtil.toLocal( this, side.getOpposite() ) );
        return !isRedstoneBlockedOnSide( localDir );
    }

    @Override
    public int getRedstoneOutput( EnumFacing side )
    {
        int localDir = remapLocalSide( DirectionUtil.toLocal( this, side ) );
        if( !isRedstoneBlockedOnSide( localDir ) && getWorld() != null && !getWorld().isRemote )
        {
            ServerComputer computer = getServerComputer();
            if( computer != null ) return computer.getRedstoneOutput( localDir );
        }
        return 0;
    }

    @Override
    public boolean getBundledRedstoneConnectivity( @Nonnull EnumFacing side )
    {
        int localDir = remapLocalSide( DirectionUtil.toLocal( this, side ) );
        return !isRedstoneBlockedOnSide( localDir );
    }

    @Override
    public int getBundledRedstoneOutput( @Nonnull EnumFacing side )
    {
        int localDir = remapLocalSide( DirectionUtil.toLocal( this, side ) );
        if( !isRedstoneBlockedOnSide( localDir ) )
        {
            if( !getWorld().isRemote )
            {
                ServerComputer computer = getServerComputer();
                if( computer != null )
                {
                    return computer.getBundledRedstoneOutput( localDir );
                }
            }
        }
        return 0;
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        updateInput( neighbour );
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
    public NBTTagCompound writeToNBT( NBTTagCompound nbt )
    {
        nbt = super.writeToNBT( nbt );

        // Save ID, label and power state
        if( m_computerID >= 0 )
        {
            nbt.setInteger( "computerID", m_computerID );
        }
        if( m_label != null )
        {
            nbt.setString( "label", m_label );
        }
        nbt.setBoolean( "on", m_on );
        return nbt;
    }

    @Override
    public void readFromNBT( NBTTagCompound nbt )
    {
        super.readFromNBT( nbt );

        // Load ID
        int id = -1;
        if( nbt.hasKey( "computerID" ) )
        {
            // Post-1.6 computers
            id = nbt.getInteger( "computerID" );
        }
        else if( nbt.hasKey( "userDir" ) )
        {
            // Pre-1.6 computers
            String userDir = nbt.getString( "userDir" );
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
        m_label = nbt.hasKey( "label" ) ? nbt.getString( "label" ) : null;

        // Load power state
        m_startOn = nbt.getBoolean( "on" );
        m_on = m_startOn;
    }

    protected boolean isPeripheralBlockedOnSide( int localSide )
    {
        return false;
    }

    protected boolean isRedstoneBlockedOnSide( int localSide )
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
        if( !isRedstoneBlockedOnSide( localDir ) )
        {
            computer.setRedstoneInput( localDir, getWorld().getRedstonePower( offset, dir ) );
            computer.setBundledRedstoneInput( localDir, BundledRedstone.getOutput( getWorld(), offset, offsetSide ) );
        }
        if( !isPeripheralBlockedOnSide( localDir ) )
        {
            computer.setPeripheral( localDir, Peripherals.getPeripheral( getWorld(), offset, offsetSide ) );
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

    // IComputerTile

    @Override
    public int getComputerID()
    {
        return m_computerID;
    }

    @Override
    public String getLabel()
    {
        return m_label;
    }

    @Override
    public void setComputerID( int id )
    {
        if( getWorld().isRemote || m_computerID == id ) return;

        m_computerID = id;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setID( m_computerID );
        markDirty();
    }

    @Override
    public void setLabel( String label )
    {
        if( getWorld().isRemote || Objects.equals( m_label, label ) ) return;

        m_label = label;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setLabel( label );
        markDirty();
    }

    @Override
    public ComputerFamily getFamily()
    {
        BlockComputerBase block = getBlock();
        if( block != null )
        {
            return block.getFamily( getWorld(), getPos() );
        }
        return ComputerFamily.Normal;
    }

    @Override
    @Deprecated
    public IComputer getComputer()
    {
        return getWorld().isRemote ? getClientComputer() : getServerComputer();
    }

    public ServerComputer createServerComputer()
    {
        if( getWorld().isRemote ) return null;

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

    public ServerComputer getServerComputer()
    {
        return getWorld().isRemote ? null : ComputerCraft.serverComputerRegistry.get( m_instanceID );
    }

    public ClientComputer createClientComputer()
    {
        if( !getWorld().isRemote || m_instanceID < 0 ) return null;

        if( !ComputerCraft.clientComputerRegistry.contains( m_instanceID ) )
        {
            ComputerCraft.clientComputerRegistry.add( m_instanceID, new ClientComputer( m_instanceID ) );
        }
        return ComputerCraft.clientComputerRegistry.get( m_instanceID );
    }

    public ClientComputer getClientComputer()
    {
        return getWorld().isRemote ? ComputerCraft.clientComputerRegistry.get( m_instanceID ) : null;
    }

    // Networking stuff

    @Override
    public void writeDescription( @Nonnull NBTTagCompound nbt )
    {
        super.writeDescription( nbt );
        nbt.setInteger( "instanceID", createServerComputer().getInstanceID() );
        if( m_label != null ) nbt.setString( "label", m_label );
        if( m_computerID >= 0 ) nbt.setInteger( "computerID", m_computerID );
    }

    @Override
    public void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        m_instanceID = nbt.getInteger( "instanceID" );
        m_label = nbt.hasKey( "label" ) ? nbt.getString( "label" ) : null;
        m_computerID = nbt.hasKey( "computerID" ) ? nbt.getInteger( "computerID" ) : -1;
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

    @Nullable
    @Override
    public IPeripheral getPeripheral( @Nonnull EnumFacing side )
    {
        return new ComputerPeripheral( "computer", createProxy() );
    }
}
