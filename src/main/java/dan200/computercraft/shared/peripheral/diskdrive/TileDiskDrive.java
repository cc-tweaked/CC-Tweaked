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
import dan200.computercraft.shared.MediaProviders;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.IPeripheralTile;
import dan200.computercraft.shared.util.DefaultInventory;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.NamedTileEntityType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Tickable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TileDiskDrive extends TileGeneric implements DefaultInventory, Tickable, IPeripheralTile
{
    private static final String NBT_ITEM = "Item";

    public static final NamedTileEntityType<TileDiskDrive> FACTORY = NamedTileEntityType.create(
        new Identifier( ComputerCraft.MOD_ID, "disk_drive" ),
        TileDiskDrive::new
    );

    public static final int INVENTORY_SIZE = 1;

    private static class MountInfo
    {
        String mountPath;
    }

    private final Map<IComputerAccess, MountInfo> computers = new HashMap<>();

    @Nonnull
    private ItemStack m_diskStack = ItemStack.EMPTY;
    private IMount m_diskMount;

    private boolean m_recordQueued;
    private boolean m_recordPlaying;
    private boolean m_restartRecord;
    private boolean m_ejectQueued;

    public TileDiskDrive( BlockEntityType<? extends TileDiskDrive> type )
    {
        super( type );
    }

    @Override
    public void destroy()
    {
        if( m_recordPlaying ) stopRecord();
    }

    @Override
    public boolean onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        if( player.isSneaking() )
        {
            // Try to put a disk into the drive
            if( !getWorld().isClient )
            {
                ItemStack disk = player.getStackInHand( hand );
                if( !disk.isEmpty() && getDiskStack().isEmpty() && MediaProviders.get( disk ) != null )
                {
                    setDiskStack( disk );
                    player.setStackInHand( hand, ItemStack.EMPTY );
                    return true;
                }
            }
        }
        else
        {
            // Open the GUI
            if( !getWorld().isClient ) ComputerCraft.openDiskDriveGUI( player, this );
            return true;
        }
        return false;
    }

    public Direction getDirection()
    {
        return getCachedState().get( BlockDiskDrive.FACING );
    }

    @Override
    public void fromTag( CompoundTag nbt )
    {
        super.fromTag( nbt );
        if( nbt.containsKey( NBT_ITEM ) )
        {
            CompoundTag item = nbt.getCompound( NBT_ITEM );
            m_diskStack = ItemStack.fromTag( item );
            m_diskMount = null;
        }
    }

    @Nonnull
    @Override
    public CompoundTag toTag( CompoundTag nbt )
    {
        nbt = super.toTag( nbt );
        if( !m_diskStack.isEmpty() )
        {
            CompoundTag item = new CompoundTag();
            m_diskStack.toTag( item );
            nbt.put( NBT_ITEM, item );
        }
        return nbt;
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
            if( !world.isClient && m_recordPlaying != m_recordQueued || m_restartRecord )
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

    // Inventory implementation

    @Override
    public int getInvSize()
    {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isInvEmpty()
    {
        return m_diskStack.isEmpty();
    }

    @Override
    public ItemStack getInvStack( int slot )
    {
        return m_diskStack;
    }

    @Nonnull
    @Override
    public ItemStack removeInvStack( int i )
    {
        ItemStack result = m_diskStack;
        m_diskStack = ItemStack.EMPTY;
        m_diskMount = null;

        return result;
    }

    @Nonnull
    @Override
    public ItemStack takeInvStack( int i, int j )
    {
        if( m_diskStack.isEmpty() ) return ItemStack.EMPTY;

        if( m_diskStack.getAmount() <= j )
        {
            ItemStack disk = m_diskStack;
            setInvStack( 0, ItemStack.EMPTY );
            return disk;
        }

        ItemStack part = m_diskStack.split( j );
        setInvStack( 0, m_diskStack.isEmpty() ? ItemStack.EMPTY : m_diskStack );
        return part;
    }

    @Override
    public void setInvStack( int slot, ItemStack itemStack )
    {
        if( getWorld().isClient )
        {
            m_diskStack = itemStack;
            m_diskMount = null;
            markDirty();
            return;
        }

        synchronized( this )
        {
            if( InventoryUtil.areItemsStackable( itemStack, m_diskStack ) )
            {
                m_diskStack = itemStack;
                return;
            }

            // Unmount old disk
            if( !m_diskStack.isEmpty() )
            {
                // TODO: Is this iteration thread safe?
                Set<IComputerAccess> computers = this.computers.keySet();
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
            m_diskStack = itemStack;
            m_diskMount = null;
            markDirty();

            // Mount new disk
            if( !m_diskStack.isEmpty() )
            {
                Set<IComputerAccess> computers = this.computers.keySet();
                for( IComputerAccess computer : computers ) mountDisk( computer );
            }
        }
    }

    @Override
    public void markDirty()
    {
        if( !world.isClient ) updateBlockState();
        super.markDirty();
    }

    @Override
    public boolean canPlayerUseInv( PlayerEntity player )
    {
        return isUsable( player, false );
    }

    @Override
    public void clear()
    {
        setInvStack( 0, ItemStack.EMPTY );
    }

    // IPeripheralTile implementation

    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        return new DiskDrivePeripheral( this );
    }

    @Nonnull
    public ItemStack getDiskStack()
    {
        return getInvStack( 0 );
    }

    public void setDiskStack( @Nonnull ItemStack stack )
    {
        setInvStack( 0, stack );
    }

    public IMedia getDiskMedia()
    {
        return MediaProviders.get( getDiskStack() );
    }

    public String getDiskMountPath( IComputerAccess computer )
    {
        synchronized( this )
        {
            if( computers.containsKey( computer ) )
            {
                MountInfo info = computers.get( computer );
                return info.mountPath;
            }
        }
        return null;
    }

    public void mount( IComputerAccess computer )
    {
        synchronized( this )
        {
            computers.put( computer, new MountInfo() );
            mountDisk( computer );
        }
    }

    public void unmount( IComputerAccess computer )
    {
        synchronized( this )
        {
            unmountDisk( computer );
            computers.remove( computer );
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
            MountInfo info = computers.get( computer );
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
            MountInfo info = computers.get( computer );
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
        BlockState blockState = getCachedState();
        if( blockState.get( BlockDiskDrive.STATE ) == state ) return;

        getWorld().setBlockState( getPos(), blockState.with( BlockDiskDrive.STATE, state ) );
    }

    private synchronized void ejectContents( boolean destroyed )
    {
        if( getWorld().isClient )
        {
            return;
        }

        if( !m_diskStack.isEmpty() )
        {
            // Remove the disks from the inventory
            ItemStack disks = m_diskStack;
            setDiskStack( ItemStack.EMPTY );

            // Spawn the item in the world
            int xOff = 0;
            int zOff = 0;
            if( !destroyed )
            {
                Direction dir = getDirection();
                xOff = dir.getOffsetX();
                zOff = dir.getOffsetZ();
            }

            BlockPos pos = getPos();
            double x = pos.getX() + 0.5 + xOff * 0.5;
            double y = pos.getY() + 0.75;
            double z = pos.getZ() + 0.5 + zOff * 0.5;
            ItemEntity entityitem = new ItemEntity( getWorld(), x, y, z, disks );
            entityitem.setVelocity( xOff * 0.15, 0.0, zOff * 0.15 );

            getWorld().spawnEntity( entityitem );
            if( !destroyed )
            {
                getWorld().playEvent( 1000, getPos(), 0 ); // BLOCK_DISPENSER_DISPENSE (See Renderer)
            }
        }
    }

    @Override
    public final void readDescription( @Nonnull CompoundTag nbt )
    {
        super.readDescription( nbt );
        m_diskStack = nbt.containsKey( NBT_ITEM ) ? ItemStack.fromTag( nbt.getCompound( NBT_ITEM ) ) : ItemStack.EMPTY;
        updateBlock();
    }

    @Override
    public void writeDescription( @Nonnull CompoundTag nbt )
    {
        super.writeDescription( nbt );
        if( !m_diskStack.isEmpty() )
        {
            CompoundTag item = new CompoundTag();
            m_diskStack.toTag( item );
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
            ComputerCraft.playRecord( record, contents.getAudioTitle( m_diskStack ), getWorld(), getPos() );
        }
        else
        {
            ComputerCraft.playRecord( null, null, getWorld(), getPos() );
        }
    }

    private void stopRecord()
    {
        ComputerCraft.playRecord( null, null, getWorld(), getPos() );
    }
}
