/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.BundledRedstone;
import dan200.computercraft.shared.Peripherals;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.RedstoneUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.INameable;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class TileComputerBase extends TileGeneric implements IComputerTile, ITickable, INameable
{
    private static final String NBT_ID = "ComputerId";
    private static final String NBT_LABEL = "Label";
    private static final String NBT_INSTANCE = "InstanceId";
    private static final String NBT_ON = "On";

    private int m_instanceID = -1;
    private int m_computerID = -1;
    protected String m_label = null;
    private boolean m_on = false;
    boolean m_startOn = false;
    private boolean m_fresh = false;

    private final ComputerFamily family;

    public TileComputerBase( TileEntityType<? extends TileGeneric> type, ComputerFamily family )
    {
        super( type );
        this.family = family;
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
        for( EnumFacing dir : DirectionUtil.FACINGS )
        {
            RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
        }
    }

    @Override
    public void onChunkUnloaded()
    {
        unload();
    }

    @Override
    public void remove()
    {
        unload();
        super.remove();
    }

    public abstract void openGUI( EntityPlayer player );

    protected boolean canNameWithTag( EntityPlayer player )
    {
        return false;
    }

    protected boolean onDefaultComputerInteract( EntityPlayer player )
    {
        if( !getWorld().isRemote && isUsable( player, false ) )
        {
            createServerComputer().turnOn();
            openGUI( player );
        }
        return true;
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        ItemStack item = player.getHeldItem( hand );
        if( !item.isEmpty() && item.getItem() == Items.NAME_TAG && canNameWithTag( player ) )
        {
            // Label to rename computer
            if( !getWorld().isRemote && item.hasDisplayName() )
            {
                setLabel( item.getDisplayName().getUnformattedComponentText() );
                item.shrink( 1 );
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
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        updateSideInput( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        updateSideInput( neighbour );
    }

    @Override
    public void tick()
    {
        if( getWorld().isRemote ) return;
        ServerComputer computer = createServerComputer();
        if( computer == null ) return;

        // If the computer isn't on and should be, then turn it on
        if( m_startOn || (m_fresh && m_on) )
        {
            computer.turnOn();
            m_startOn = false;
        }

        computer.keepAlive();

        m_fresh = false;
        m_computerID = computer.getID();
        m_label = computer.getLabel();
        m_on = computer.isOn();

        // Update the block state if needed. We don't fire a block update intentionally,
        // as this only really is needed on the client side.
        updateBlockState( computer.getState() );

        if( computer.hasOutputChanged() ) updateOutput();
    }

    protected abstract void updateBlockState( ComputerState newState );

    @Nonnull
    @Override
    public NBTTagCompound write( NBTTagCompound nbt )
    {
        // Save id, label and power state
        if( m_computerID >= 0 ) nbt.putInt( NBT_ID, m_computerID );
        if( m_label != null ) nbt.putString( NBT_LABEL, m_label );
        nbt.putBoolean( NBT_ON, m_on );

        return super.write( nbt );
    }

    @Override
    public void read( NBTTagCompound nbt )
    {
        super.read( nbt );

        // Load ID, label and power state
        m_computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
        m_label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        m_on = m_startOn = nbt.getBoolean( NBT_ON );
    }

    protected boolean isPeripheralBlockedOnSide( EnumFacing localSide )
    {
        return false;
    }

    protected boolean isRedstoneBlockedOnSide( EnumFacing localSide )
    {
        return false;
    }

    protected abstract EnumFacing getDirection();

    protected EnumFacing remapToLocalSide( EnumFacing globalSide )
    {
        return DirectionUtil.toLocal( getDirection(), globalSide );
    }

    private void updateSideInput( ServerComputer computer, EnumFacing dir, BlockPos offset )
    {
        EnumFacing offsetSide = dir.getOpposite();
        EnumFacing localDir = remapToLocalSide( dir );
        if( !isRedstoneBlockedOnSide( localDir ) )
        {
            computer.setRedstoneInput( localDir.getIndex(), getWorld().getRedstonePower( offset, dir ) );
            computer.setBundledRedstoneInput( localDir.getIndex(), BundledRedstone.getOutput( getWorld(), offset, offsetSide ) );
        }
        if( !isPeripheralBlockedOnSide( localDir ) )
        {
            computer.setPeripheral( localDir.getIndex(), Peripherals.getPeripheral( getWorld(), offset, offsetSide ) );
        }
    }

    public void updateAllInputs()
    {
        if( getWorld() == null || getWorld().isRemote ) return;

        // Update all sides
        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        BlockPos pos = computer.getPosition();
        for( EnumFacing dir : DirectionUtil.FACINGS )
        {
            updateSideInput( computer, dir, pos.offset( dir ) );
        }
    }

    private void updateSideInput( BlockPos neighbour )
    {
        if( getWorld() == null || getWorld().isRemote ) return;

        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        // Find the appropriate side and update.
        BlockPos pos = computer.getPosition();
        for( EnumFacing dir : DirectionUtil.FACINGS )
        {
            BlockPos offset = pos.offset( dir );
            if( offset.equals( neighbour ) )
            {
                updateSideInput( computer, dir, offset );
                break;
            }
        }
    }

    public void updateOutput()
    {
        // Update redstone
        updateBlock();
        for( EnumFacing dir : DirectionUtil.FACINGS )
        {
            RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
        }
    }

    protected abstract ServerComputer createComputer( int instanceID, int id );

    public abstract ComputerProxy createProxy();

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
        return family;
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
            updateAllInputs();
        }
        return ComputerCraft.serverComputerRegistry.get( m_instanceID );
    }

    public ServerComputer getServerComputer()
    {
        return !getWorld().isRemote ? ComputerCraft.serverComputerRegistry.get( m_instanceID ) : null;
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

        // The client needs to know about the computer ID and label in order to provide pick-block
        // functionality
        if( m_computerID >= 0 ) nbt.putInt( NBT_ID, m_computerID );
        if( m_label != null ) nbt.putString( NBT_LABEL, m_label );
        nbt.putInt( NBT_INSTANCE, createServerComputer().getInstanceID() );
    }

    @Override
    protected void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        m_instanceID = nbt.contains( NBT_INSTANCE ) ? nbt.getInt( NBT_INSTANCE ) : -1;
        m_label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        m_computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
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

    @Nonnull
    @Override
    public ITextComponent getName()
    {
        String label = getLabel();
        return label == null ? getBlockState().getBlock().getNameTextComponent() : new TextComponentString( label );
    }

    @Override
    public boolean hasCustomName()
    {
        String label = getLabel();
        return label != null && label.isEmpty();
    }

    @Nullable
    @Override
    public ITextComponent getCustomName()
    {
        String label = getLabel();
        return label == null ? null : new TextComponentString( label );
    }
}
