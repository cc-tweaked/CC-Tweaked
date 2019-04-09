/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.MediaProviders;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.util.DefaultInventory;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.NamedBlockEntityType;
import dan200.computercraft.shared.util.RecordUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public final class TileDiskDrive extends TileGeneric implements DefaultInventory, ITickable, IPeripheralTile
{
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_ITEM = "Item";

    public static final NamedBlockEntityType<TileDiskDrive> FACTORY = NamedBlockEntityType.create(
        new ResourceLocation( ComputerCraft.MOD_ID, "disk_drive" ),
        TileDiskDrive::new
    );

    private static class MountInfo
    {
        String mountPath;
    }

    ITextComponent customName;

    private final Map<IComputerAccess, MountInfo> m_computers = new HashMap<>();

    @Nonnull
    private ItemStack m_diskStack = ItemStack.EMPTY;
    private LazyOptional<IItemHandlerModifiable> itemHandlerCap;
    private IMount m_diskMount = null;

    private boolean m_recordQueued = false;
    private boolean m_recordPlaying = false;
    private boolean m_restartRecord = false;
    private boolean m_ejectQueued;

    private TileDiskDrive()
    {
        super( FACTORY );
    }

    @Override
    public void destroy()
    {
        ejectContents( true );
        if( m_recordPlaying ) stopRecord();
    }

    @Override
    protected void invalidateCaps()
    {
        super.invalidateCaps();
        if( itemHandlerCap != null )
        {
            itemHandlerCap.invalidate();
            itemHandlerCap = null;
        }
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        if( player.isSneaking() )
        {
            // Try to put a disk into the drive
            ItemStack disk = player.getHeldItem( hand );
            if( disk.isEmpty() ) return false;
            if( !getWorld().isRemote && getStackInSlot( 0 ).isEmpty() && MediaProviders.get( disk ) != null )
            {
                setDiskStack( disk );
                player.setHeldItem( hand, ItemStack.EMPTY );
            }
            return true;
        }
        else
        {
            // Open the GUI
            if( !getWorld().isRemote ) Containers.openDiskDriveGUI( player, this );
            return true;
        }
    }

    public EnumFacing getDirection()
    {
        return getBlockState().get( BlockDiskDrive.FACING );
    }

    @Override
    public void read( NBTTagCompound nbt )
    {
        super.read( nbt );
        customName = nbt.contains( NBT_NAME ) ? ITextComponent.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;
        if( nbt.contains( NBT_ITEM ) )
        {
            NBTTagCompound item = nbt.getCompound( NBT_ITEM );
            m_diskStack = ItemStack.read( item );
            m_diskMount = null;
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound write( NBTTagCompound nbt )
    {
        if( customName != null ) nbt.putString( NBT_NAME, ITextComponent.Serializer.toJson( customName ) );

        if( !m_diskStack.isEmpty() )
        {
            NBTTagCompound item = new NBTTagCompound();
            m_diskStack.write( item );
            nbt.put( NBT_ITEM, item );
        }
        return super.write( nbt );
    }

    @Override
    public void tick()
    {
        // Ejection
        if( m_ejectQueued )
        {
            ejectContents( false );
            m_ejectQueued = false;
        }

        // Music
        synchronized( this )
        {
            if( !world.isRemote && m_recordPlaying != m_recordQueued || m_restartRecord )
            {
                m_restartRecord = false;
                if( m_recordQueued )
                {
                    IMedia contents = getDiskMedia();
                    SoundEvent record = contents != null ? contents.getAudio( m_diskStack ) : null;
                    if( record != null )
                    {
                        m_recordPlaying = true;
                        playRecord();
                    }
                    else
                    {
                        m_recordQueued = false;
                    }
                }
                else
                {
                    stopRecord();
                    m_recordPlaying = false;
                }
            }
        }
    }

    // IInventory implementation

    @Override
    public int getSizeInventory()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return m_diskStack.isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot( int slot )
    {
        return m_diskStack;
    }

    @Nonnull
    @Override
    public ItemStack removeStackFromSlot( int slot )
    {
        ItemStack result = m_diskStack;
        m_diskStack = ItemStack.EMPTY;
        m_diskMount = null;

        return result;
    }

    @Nonnull
    @Override
    public ItemStack decrStackSize( int slot, int count )
    {
        if( m_diskStack.isEmpty() ) return ItemStack.EMPTY;

        if( m_diskStack.getCount() <= count )
        {
            ItemStack disk = m_diskStack;
            setInventorySlotContents( slot, ItemStack.EMPTY );
            return disk;
        }

        ItemStack part = m_diskStack.split( count );
        setInventorySlotContents( slot, m_diskStack.isEmpty() ? ItemStack.EMPTY : m_diskStack );
        return part;
    }

    @Override
    public void setInventorySlotContents( int slot, @Nonnull ItemStack stack )
    {
        if( getWorld().isRemote )
        {
            m_diskStack = stack;
            m_diskMount = null;
            markDirty();
            return;
        }

        synchronized( this )
        {
            if( InventoryUtil.areItemsStackable( stack, m_diskStack ) )
            {
                m_diskStack = stack;
                return;
            }

            // Unmount old disk
            if( !m_diskStack.isEmpty() )
            {
                // TODO: Is this iteration thread safe?
                Set<IComputerAccess> computers = m_computers.keySet();
                for( IComputerAccess computer : computers ) unmountDisk( computer );
            }

            // Stop music
            if( m_recordPlaying )
            {
                stopRecord();
                m_recordPlaying = false;
                m_recordQueued = false;
            }

            // Swap disk over
            m_diskStack = stack;
            m_diskMount = null;
            markDirty();

            // Mount new disk
            if( !m_diskStack.isEmpty() )
            {
                Set<IComputerAccess> computers = m_computers.keySet();
                for( IComputerAccess computer : computers ) mountDisk( computer );
            }
        }
    }

    @Override
    public void markDirty()
    {
        if( !world.isRemote ) updateBlockState();
        super.markDirty();
    }

    @Override
    public boolean isUsableByPlayer( @Nonnull EntityPlayer player )
    {
        return isUsable( player, false );
    }

    @Override
    public void clear()
    {
        setInventorySlotContents( 0, ItemStack.EMPTY );
    }

    @Override
    public IPeripheral getPeripheral( @Nonnull EnumFacing side )
    {
        return new DiskDrivePeripheral( this );
    }

    @Nonnull
    public ItemStack getDiskStack()
    {
        return getStackInSlot( 0 );
    }

    public void setDiskStack( @Nonnull ItemStack stack )
    {
        setInventorySlotContents( 0, stack );
    }

    public IMedia getDiskMedia()
    {
        return MediaProviders.get( getDiskStack() );
    }

    public String getDiskMountPath( IComputerAccess computer )
    {
        synchronized( this )
        {
            if( m_computers.containsKey( computer ) )
            {
                MountInfo info = m_computers.get( computer );
                return info.mountPath;
            }
        }
        return null;
    }

    public void mount( IComputerAccess computer )
    {
        synchronized( this )
        {
            m_computers.put( computer, new MountInfo() );
            mountDisk( computer );
        }
    }

    public void unmount( IComputerAccess computer )
    {
        synchronized( this )
        {
            unmountDisk( computer );
            m_computers.remove( computer );
        }
    }

    public void playDiskAudio()
    {
        synchronized( this )
        {
            IMedia media = getDiskMedia();
            if( media != null && media.getAudioTitle( m_diskStack ) != null )
            {
                m_recordQueued = true;
                m_restartRecord = m_recordPlaying;
            }
        }
    }

    public void stopDiskAudio()
    {
        synchronized( this )
        {
            m_recordQueued = false;
            m_restartRecord = false;
        }
    }

    public void ejectDisk()
    {
        synchronized( this )
        {
            m_ejectQueued = true;
        }
    }

    // private methods

    private synchronized void mountDisk( IComputerAccess computer )
    {
        if( !m_diskStack.isEmpty() )
        {
            MountInfo info = m_computers.get( computer );
            IMedia contents = getDiskMedia();
            if( contents != null )
            {
                if( m_diskMount == null )
                {
                    m_diskMount = contents.createDataMount( m_diskStack, getWorld() );
                }
                if( m_diskMount != null )
                {
                    if( m_diskMount instanceof IWritableMount )
                    {
                        // Try mounting at the lowest numbered "disk" name we can
                        int n = 1;
                        while( info.mountPath == null )
                        {
                            info.mountPath = computer.mountWritable( n == 1 ? "disk" : "disk" + n, (IWritableMount) m_diskMount );
                            n++;
                        }
                    }
                    else
                    {
                        // Try mounting at the lowest numbered "disk" name we can
                        int n = 1;
                        while( info.mountPath == null )
                        {
                            info.mountPath = computer.mount( n == 1 ? "disk" : "disk" + n, m_diskMount );
                            n++;
                        }
                    }
                }
                else
                {
                    info.mountPath = null;
                }
            }
            computer.queueEvent( "disk", new Object[] { computer.getAttachmentName() } );
        }
    }

    private synchronized void unmountDisk( IComputerAccess computer )
    {
        if( !m_diskStack.isEmpty() )
        {
            MountInfo info = m_computers.get( computer );
            assert info != null;
            if( info.mountPath != null )
            {
                computer.unmount( info.mountPath );
                info.mountPath = null;
            }
            computer.queueEvent( "disk_eject", new Object[] { computer.getAttachmentName() } );
        }
    }

    private void updateBlockState()
    {
        if( removed ) return;

        if( !m_diskStack.isEmpty() )
        {
            IMedia contents = getDiskMedia();
            updateBlockState( contents != null ? DiskDriveState.FULL : DiskDriveState.INVALID );
        }
        else
        {
            updateBlockState( DiskDriveState.EMPTY );
        }
    }

    private void updateBlockState( DiskDriveState state )
    {
        IBlockState blockState = getBlockState();
        if( blockState.get( BlockDiskDrive.STATE ) == state ) return;

        getWorld().setBlockState( getPos(), blockState.with( BlockDiskDrive.STATE, state ) );
    }

    private synchronized void ejectContents( boolean destroyed )
    {
        if( getWorld().isRemote || m_diskStack.isEmpty() ) return;

        // Remove the disks from the inventory
        ItemStack disks = m_diskStack;
        setDiskStack( ItemStack.EMPTY );

        // Spawn the item in the world
        int xOff = 0;
        int zOff = 0;
        if( !destroyed )
        {
            EnumFacing dir = getDirection();
            xOff = dir.getXOffset();
            zOff = dir.getZOffset();
        }

        BlockPos pos = getPos();
        double x = pos.getX() + 0.5 + xOff * 0.5;
        double y = pos.getY() + 0.75;
        double z = pos.getZ() + 0.5 + zOff * 0.5;
        EntityItem entityitem = new EntityItem( getWorld(), x, y, z, disks );
        entityitem.motionX = xOff * 0.15;
        entityitem.motionY = 0.0;
        entityitem.motionZ = zOff * 0.15;

        getWorld().spawnEntity( entityitem );
        if( !destroyed ) getWorld().playBroadcastSound( 1000, getPos(), 0 );
    }

    @Override
    protected void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        customName = nbt.contains( NBT_NAME ) ? ITextComponent.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;
        m_diskStack = nbt.contains( NBT_ITEM ) ? ItemStack.read( nbt.getCompound( NBT_ITEM ) ) : ItemStack.EMPTY;
        updateBlock();
    }

    @Override
    protected void writeDescription( @Nonnull NBTTagCompound nbt )
    {
        super.writeDescription( nbt );
        if( customName != null ) nbt.putString( NBT_NAME, ITextComponent.Serializer.toJson( customName ) );
        if( !m_diskStack.isEmpty() )
        {
            NBTTagCompound item = new NBTTagCompound();
            m_diskStack.write( item );
            nbt.put( NBT_ITEM, item );
        }
    }

    // Private methods

    private void playRecord()
    {
        IMedia contents = getDiskMedia();
        SoundEvent record = contents != null ? contents.getAudio( m_diskStack ) : null;
        if( record != null )
        {
            RecordUtil.playRecord( record, contents.getAudioTitle( m_diskStack ), getWorld(), getPos() );
        }
        else
        {
            RecordUtil.playRecord( null, null, getWorld(), getPos() );
        }
    }

    private void stopRecord()
    {
        RecordUtil.playRecord( null, null, getWorld(), getPos() );
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable final EnumFacing side )
    {
        if( cap == ITEM_HANDLER_CAPABILITY )
        {
            if( itemHandlerCap == null ) itemHandlerCap = LazyOptional.of( () -> new InvWrapper( this ) );
            return itemHandlerCap.cast();
        }
        return super.getCapability( cap, side );
    }

    @Override
    public boolean hasCustomName()
    {
        return customName != null;
    }

    @Nullable
    @Override
    public ITextComponent getCustomName()
    {
        return customName;
    }

    @Nonnull
    @Override
    public ITextComponent getName()
    {
        return customName != null ? customName : getBlockState().getBlock().getNameTextComponent();
    }
}
