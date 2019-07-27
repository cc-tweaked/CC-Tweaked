/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.core.computer.ComputerSide;
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
import joptsimple.internal.Strings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class TileComputerBase extends TileGeneric implements IComputerTile, IDirectionalTile, ITickable, IPeripheralTile, IWorldNameable
{
    private int m_instanceID = -1;
    private int m_computerID = -1;
    protected String label = null;
    private boolean m_on = false;
    boolean m_startOn = false;
    private boolean m_fresh = false;

    @Override
    public BlockComputerBase getBlock()
    {
        Block block = super.getBlock();
        return block instanceof BlockComputerBase ? (BlockComputerBase) block : null;
    }

    protected void unload()
    {
        if( m_instanceID >= 0 )
        {
            if( !getWorld().isRemote ) ComputerCraft.serverComputerRegistry.remove( m_instanceID );
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

    @Override
    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        ItemStack currentItem = player.getHeldItem( hand );
        if( !currentItem.isEmpty() && currentItem.getItem() == Items.NAME_TAG && canNameWithTag( player ) && currentItem.hasDisplayName() )
        {
            // Label to rename computer
            if( !getWorld().isRemote )
            {
                setLabel( currentItem.getDisplayName() );
                currentItem.shrink( 1 );
            }
            return true;
        }
        else if( !player.isSneaking() )
        {
            // Regular right click to activate computer
            if( !getWorld().isRemote && isUsable( player, false ) )
            {
                createServerComputer().turnOn();
                openGUI( player );
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean getRedstoneConnectivity( EnumFacing side )
    {
        return true;
    }

    @Override
    public int getRedstoneOutput( EnumFacing side )
    {
        ComputerSide localDir = remapLocalSide( DirectionUtil.toLocal( this, side ) );
        if( world != null && !world.isRemote )
        {
            ServerComputer computer = getServerComputer();
            if( computer != null ) return computer.getRedstoneOutput( localDir );
        }
        return 0;
    }

    @Override
    public boolean getBundledRedstoneConnectivity( @Nonnull EnumFacing side )
    {
        return true;
    }

    @Override
    public int getBundledRedstoneOutput( @Nonnull EnumFacing side )
    {
        ComputerSide localDir = remapLocalSide( DirectionUtil.toLocal( this, side ) );
        if( !world.isRemote )
        {
            ServerComputer computer = getServerComputer();
            if( computer != null ) return computer.getBundledRedstoneOutput( localDir );
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

                m_fresh = false;
                m_computerID = computer.getID();
                label = computer.getLabel();
                m_on = computer.isOn();

                if( computer.hasOutputChanged() ) updateOutput();
            }
        }
        else
        {
            ClientComputer computer = createClientComputer();
            if( computer != null && computer.hasOutputChanged() ) updateBlock();
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound nbt )
    {
        // Save ID, label and power state
        if( m_computerID >= 0 )
        {
            nbt.setInteger( "computerID", m_computerID );
        }
        if( label != null )
        {
            nbt.setString( "label", label );
        }
        nbt.setBoolean( "on", m_on );
        return super.writeToNBT( nbt );
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
        label = nbt.hasKey( "label" ) ? nbt.getString( "label" ) : null;

        // Load power state
        m_startOn = nbt.getBoolean( "on" );
        m_on = m_startOn;
    }

    protected boolean isPeripheralBlockedOnSide( ComputerSide localSide )
    {
        return false;
    }

    protected ComputerSide remapLocalSide( ComputerSide localSide )
    {
        return localSide;
    }

    private void updateSideInput( ServerComputer computer, EnumFacing dir, BlockPos offset )
    {
        EnumFacing offsetSide = dir.getOpposite();
        ComputerSide localDir = remapLocalSide( DirectionUtil.toLocal( this, dir ) );

        computer.setRedstoneInput( localDir, getRedstoneInput( world, offset, dir ) );
        computer.setBundledRedstoneInput( localDir, BundledRedstone.getOutput( getWorld(), offset, offsetSide ) );

        if( !isPeripheralBlockedOnSide( localDir ) )
        {
            computer.setPeripheral( localDir, Peripherals.getPeripheral( getWorld(), offset, offsetSide ) );
        }
    }

    /**
     * Gets the redstone input for an adjacent block
     *
     * @param world The world we exist in
     * @param pos   The position of the neighbour
     * @param side  The side we are reading from
     * @return The effective redstone power
     * @see net.minecraft.block.BlockRedstoneDiode#calculateInputStrength(World, BlockPos, IBlockState)
     */
    protected static int getRedstoneInput( World world, BlockPos pos, EnumFacing side )
    {
        int power = world.getRedstonePower( pos, side );
        if( power >= 15 ) return power;

        IBlockState neighbour = world.getBlockState( pos );
        return neighbour.getBlock() == Blocks.REDSTONE_WIRE
            ? Math.max( power, neighbour.getValue( BlockRedstoneWire.POWER ) )
            : power;
    }

    public void updateInput()
    {
        if( getWorld() == null || getWorld().isRemote ) return;

        // Update redstone and peripherals
        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        BlockPos pos = computer.getPosition();
        for( EnumFacing dir : EnumFacing.VALUES )
        {
            updateSideInput( computer, dir, pos.offset( dir ) );
        }
    }

    public void updateInput( BlockPos neighbour )
    {
        if( getWorld() == null || getWorld().isRemote ) return;

        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        BlockPos pos = computer.getPosition();
        for( EnumFacing dir : EnumFacing.VALUES )
        {
            BlockPos offset = pos.offset( dir );
            if( offset.equals( neighbour ) )
            {
                updateSideInput( computer, dir, offset );
                return;
            }
        }

        // If the position is not any adjacent one, update all inputs.
        updateInput();
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
    public final int getComputerID()
    {
        return m_computerID;
    }

    @Override
    public final String getLabel()
    {
        return label;
    }

    @Override
    public final void setComputerID( int id )
    {
        if( getWorld().isRemote || m_computerID == id ) return;

        m_computerID = id;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setID( m_computerID );
        markDirty();
    }

    @Override
    public final void setLabel( String label )
    {
        if( getWorld().isRemote || Objects.equals( this.label, label ) ) return;

        this.label = label;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setLabel( label );
        markDirty();
    }

    @Override
    public ComputerFamily getFamily()
    {
        BlockComputerBase block = getBlock();
        return block != null ? block.getFamily( getWorld(), getPos() ) : ComputerFamily.Normal;
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

        ClientComputer computer = ComputerCraft.clientComputerRegistry.get( m_instanceID );
        if( computer == null )
        {
            ComputerCraft.clientComputerRegistry.add( m_instanceID, computer = new ClientComputer( m_instanceID ) );
        }
        return computer;
    }

    public ClientComputer getClientComputer()
    {
        return getWorld().isRemote ? ComputerCraft.clientComputerRegistry.get( m_instanceID ) : null;
    }

    // Networking stuff

    @Override
    protected void writeDescription( @Nonnull NBTTagCompound nbt )
    {
        super.writeDescription( nbt );
        if( m_instanceID >= 0 ) nbt.setInteger( "instanceID", m_instanceID );
        if( label != null ) nbt.setString( "label", label );
        if( m_computerID >= 0 ) nbt.setInteger( "computerID", m_computerID );
    }

    @Override
    protected void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        m_instanceID = nbt.getInteger( "instanceID" );
        label = nbt.hasKey( "label" ) ? nbt.getString( "label" ) : null;
        m_computerID = nbt.hasKey( "computerID" ) ? nbt.getInteger( "computerID" ) : -1;
    }

    protected void transferStateFrom( TileComputerBase copy )
    {
        if( copy.m_computerID != m_computerID || copy.m_instanceID != m_instanceID )
        {
            unload();
            m_instanceID = copy.m_instanceID;
            m_computerID = copy.m_computerID;
            label = copy.label;
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

    @Nonnull
    @Override
    public String getName()
    {
        return hasCustomName() ? label : getBlockType().getTranslationKey();
    }

    @Override
    public boolean hasCustomName()
    {
        return !Strings.isNullOrEmpty( label );
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName()
    {
        return hasCustomName() ? new TextComponentString( getName() ) : new TextComponentTranslation( getName() );
    }
}
