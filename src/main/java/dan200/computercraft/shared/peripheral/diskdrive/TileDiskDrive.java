/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.MediaProviders;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.util.DefaultInventory;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.RecordUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class TileDiskDrive extends TileGeneric implements DefaultInventory, Tickable, IPeripheralTile, Nameable, NamedScreenHandlerFactory
{
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_ITEM = "Item";

    private static class MountInfo
    {
        String mountPath;
    }

    Text customName;

    private final Map<IComputerAccess, MountInfo> m_computers = new HashMap<>();

    @Nonnull
    private ItemStack m_diskStack = ItemStack.EMPTY;
    private IMount m_diskMount = null;

    private boolean m_recordQueued = false;
    private boolean m_recordPlaying = false;
    private boolean m_restartRecord = false;
    private boolean m_ejectQueued;

    public TileDiskDrive( BlockEntityType<TileDiskDrive> type )
    {
        super( type );
    }

    @Override
    public void destroy()
    {
        ejectContents( true );
        if( m_recordPlaying ) stopRecord();
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        if( player.isInSneakingPose() )
        {
            // Try to put a disk into the drive
            ItemStack disk = player.getStackInHand( hand );
            if( disk.isEmpty() ) return ActionResult.PASS;
            if( !getWorld().isClient && getStack( 0 ).isEmpty() && MediaProviders.get( disk ) != null )
            {
                setDiskStack( disk );
                player.setStackInHand( hand, ItemStack.EMPTY );
            }
            return ActionResult.SUCCESS;
        }
        else
        {
            // Open the GUI
            if( !getWorld().isClient ) player.openHandledScreen( this );
            return ActionResult.SUCCESS;
        }
    }

    public Direction getDirection()
    {
        return getCachedState().get( BlockDiskDrive.FACING );
    }

    @Override
    public void fromTag( @Nonnull BlockState state, @Nonnull CompoundTag nbt )
    {
        super.fromTag( state, nbt );
        customName = nbt.contains( NBT_NAME ) ? Text.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;
        if( nbt.contains( NBT_ITEM ) )
        {
            CompoundTag item = nbt.getCompound( NBT_ITEM );
            m_diskStack = ItemStack.fromTag( item );
            m_diskMount = null;
        }
    }

    @Nonnull
    @Override
    public CompoundTag toTag( @Nonnull CompoundTag nbt )
    {
        if( customName != null ) nbt.putString( NBT_NAME, Text.Serializer.toJson( customName ) );

        if( !m_diskStack.isEmpty() )
        {
            CompoundTag item = new CompoundTag();
            m_diskStack.toTag( item );
            nbt.put( NBT_ITEM, item );
        }
        return super.toTag( nbt );
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

    // IInventory implementation

    @Override
    public int size()
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
    public ItemStack getStack( int slot )
    {
        return m_diskStack;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot )
    {
        ItemStack result = m_diskStack;
        m_diskStack = ItemStack.EMPTY;
        m_diskMount = null;

        return result;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot, int count )
    {
        if( m_diskStack.isEmpty() ) return ItemStack.EMPTY;

        if( m_diskStack.getCount() <= count )
        {
            ItemStack disk = m_diskStack;
            setStack( slot, ItemStack.EMPTY );
            return disk;
        }

        ItemStack part = m_diskStack.split( count );
        setStack( slot, m_diskStack.isEmpty() ? ItemStack.EMPTY : m_diskStack );
        return part;
    }

    @Override
    public void setStack( int slot, @Nonnull ItemStack stack )
    {
        if( getWorld().isClient )
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
        if( !world.isClient ) updateBlockState();
        super.markDirty();
    }

    @Override
    public boolean canPlayerUse( @Nonnull PlayerEntity player )
    {
        return isUsable( player, false );
    }

    @Override
    public void clear()
    {
        setStack( 0, ItemStack.EMPTY );
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral(Direction side) {
        return new DiskDrivePeripheral(this);
    }

    @Nonnull
    ItemStack getDiskStack()
    {
        return getStack( 0 );
    }

    void setDiskStack( @Nonnull ItemStack stack )
    {
        setStack( 0, stack );
    }

    private IMedia getDiskMedia()
    {
        return MediaProviders.get( getDiskStack() );
    }

    String getDiskMountPath( IComputerAccess computer )
    {
        synchronized( this )
        {
            MountInfo info = m_computers.get( computer );
            return info != null ? info.mountPath : null;
        }
    }

    void mount( IComputerAccess computer )
    {
        synchronized( this )
        {
            m_computers.put( computer, new MountInfo() );
            mountDisk( computer );
        }
    }

    void unmount( IComputerAccess computer )
    {
        synchronized( this )
        {
            unmountDisk( computer );
            m_computers.remove( computer );
        }
    }

    void playDiskAudio()
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

    void stopDiskAudio()
    {
        synchronized( this )
        {
            m_recordQueued = false;
            m_restartRecord = false;
        }
    }

    void ejectDisk()
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
            computer.queueEvent( "disk", computer.getAttachmentName() );
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
            computer.queueEvent( "disk_eject", computer.getAttachmentName() );
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
        BlockState blockState = getCachedState();
        if( blockState.get( BlockDiskDrive.STATE ) == state ) return;

        getWorld().setBlockState( getPos(), blockState.with( BlockDiskDrive.STATE, state ) );
    }

    private synchronized void ejectContents( boolean destroyed )
    {
        if( getWorld().isClient || m_diskStack.isEmpty() ) return;

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
        entityitem.setVelocity( xOff * 0.15, 0, zOff * 0.15 );

        getWorld().spawnEntity( entityitem );
        if( !destroyed ) getWorld().syncGlobalEvent( 1000, getPos(), 0 );
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

    @Override
    public boolean hasCustomName()
    {
        return customName != null;
    }

    @Nullable
    @Override
    public Text getCustomName()
    {
        return customName;
    }

    @Nonnull
    @Override
    public Text getName()
    {
        return customName != null ? customName : new TranslatableText( getCachedState().getBlock().getTranslationKey() );
    }

    @Nonnull
    @Override
    public Text getDisplayName()
    {
        return Nameable.super.getDisplayName();
    }

    @Nonnull
    @Override
    public ScreenHandler createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        return new ContainerDiskDrive( id, inventory, this );
    }
}
