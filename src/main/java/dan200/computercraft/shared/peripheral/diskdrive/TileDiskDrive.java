/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
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
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Nameable;
import net.minecraft.util.Tickable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class TileDiskDrive extends TileGeneric implements DefaultInventory, Tickable, IPeripheralTile, Nameable, NamedScreenHandlerFactory
{
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_ITEM = "Item";
    private final Map<IComputerAccess, MountInfo> m_computers = new HashMap<>();
    Text customName;
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
        this.ejectContents( true );
        if( this.m_recordPlaying )
        {
            this.stopRecord();
        }
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        if( player.isInSneakingPose() )
        {
            // Try to put a disk into the drive
            ItemStack disk = player.getStackInHand( hand );
            if( disk.isEmpty() )
            {
                return ActionResult.PASS;
            }
            if( !this.getWorld().isClient && this.getStack( 0 ).isEmpty() && MediaProviders.get( disk ) != null )
            {
                this.setDiskStack( disk );
                player.setStackInHand( hand, ItemStack.EMPTY );
            }
            return ActionResult.SUCCESS;
        }
        else
        {
            // Open the GUI
            if( !this.getWorld().isClient )
            {
                player.openHandledScreen( this );
            }
            return ActionResult.SUCCESS;
        }
    }

    public Direction getDirection()
    {
        return this.getCachedState().get( BlockDiskDrive.FACING );
    }

    @Override
    public void fromTag( @Nonnull BlockState state, @Nonnull CompoundTag nbt )
    {
        super.fromTag( state, nbt );
        this.customName = nbt.contains( NBT_NAME ) ? Text.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;
        if( nbt.contains( NBT_ITEM ) )
        {
            CompoundTag item = nbt.getCompound( NBT_ITEM );
            this.m_diskStack = ItemStack.fromTag( item );
            this.m_diskMount = null;
        }
    }

    @Nonnull
    @Override
    public CompoundTag toTag( @Nonnull CompoundTag nbt )
    {
        if( this.customName != null )
        {
            nbt.putString( NBT_NAME, Text.Serializer.toJson( this.customName ) );
        }

        if( !this.m_diskStack.isEmpty() )
        {
            CompoundTag item = new CompoundTag();
            this.m_diskStack.toTag( item );
            nbt.put( NBT_ITEM, item );
        }
        return super.toTag( nbt );
    }

    @Override
    public void markDirty()
    {
        if( !this.world.isClient )
        {
            this.updateBlockState();
        }
        super.markDirty();
    }

    @Override
    public void tick()
    {
        // Ejection
        if( this.m_ejectQueued )
        {
            this.ejectContents( false );
            this.m_ejectQueued = false;
        }

        // Music
        synchronized( this )
        {
            if( !this.world.isClient && this.m_recordPlaying != this.m_recordQueued || this.m_restartRecord )
            {
                this.m_restartRecord = false;
                if( this.m_recordQueued )
                {
                    IMedia contents = this.getDiskMedia();
                    SoundEvent record = contents != null ? contents.getAudio( this.m_diskStack ) : null;
                    if( record != null )
                    {
                        this.m_recordPlaying = true;
                        this.playRecord();
                    }
                    else
                    {
                        this.m_recordQueued = false;
                    }
                }
                else
                {
                    this.stopRecord();
                    this.m_recordPlaying = false;
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
        return this.m_diskStack.isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack getStack( int slot )
    {
        return this.m_diskStack;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot, int count )
    {
        if( this.m_diskStack.isEmpty() )
        {
            return ItemStack.EMPTY;
        }

        if( this.m_diskStack.getCount() <= count )
        {
            ItemStack disk = this.m_diskStack;
            this.setStack( slot, ItemStack.EMPTY );
            return disk;
        }

        ItemStack part = this.m_diskStack.split( count );
        this.setStack( slot, this.m_diskStack.isEmpty() ? ItemStack.EMPTY : this.m_diskStack );
        return part;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot )
    {
        ItemStack result = this.m_diskStack;
        this.m_diskStack = ItemStack.EMPTY;
        this.m_diskMount = null;

        return result;
    }

    @Override
    public void setStack( int slot, @Nonnull ItemStack stack )
    {
        if( this.getWorld().isClient )
        {
            this.m_diskStack = stack;
            this.m_diskMount = null;
            this.markDirty();
            return;
        }

        synchronized( this )
        {
            if( InventoryUtil.areItemsStackable( stack, this.m_diskStack ) )
            {
                this.m_diskStack = stack;
                return;
            }

            // Unmount old disk
            if( !this.m_diskStack.isEmpty() )
            {
                // TODO: Is this iteration thread safe?
                Set<IComputerAccess> computers = this.m_computers.keySet();
                for( IComputerAccess computer : computers )
                {
                    this.unmountDisk( computer );
                }
            }

            // Stop music
            if( this.m_recordPlaying )
            {
                this.stopRecord();
                this.m_recordPlaying = false;
                this.m_recordQueued = false;
            }

            // Swap disk over
            this.m_diskStack = stack;
            this.m_diskMount = null;
            this.markDirty();

            // Mount new disk
            if( !this.m_diskStack.isEmpty() )
            {
                Set<IComputerAccess> computers = this.m_computers.keySet();
                for( IComputerAccess computer : computers )
                {
                    this.mountDisk( computer );
                }
            }
        }
    }

    @Override
    public boolean canPlayerUse( @Nonnull PlayerEntity player )
    {
        return this.isUsable( player, false );
    }

    @Override
    public void clear()
    {
        this.setStack( 0, ItemStack.EMPTY );
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        return new DiskDrivePeripheral( this );
    }

    String getDiskMountPath( IComputerAccess computer )
    {
        synchronized( this )
        {
            MountInfo info = this.m_computers.get( computer );
            return info != null ? info.mountPath : null;
        }
    }

    void mount( IComputerAccess computer )
    {
        synchronized( this )
        {
            this.m_computers.put( computer, new MountInfo() );
            this.mountDisk( computer );
        }
    }

    private synchronized void mountDisk( IComputerAccess computer )
    {
        if( !this.m_diskStack.isEmpty() )
        {
            MountInfo info = this.m_computers.get( computer );
            IMedia contents = this.getDiskMedia();
            if( contents != null )
            {
                if( this.m_diskMount == null )
                {
                    this.m_diskMount = contents.createDataMount( this.m_diskStack, this.getWorld() );
                }
                if( this.m_diskMount != null )
                {
                    if( this.m_diskMount instanceof IWritableMount )
                    {
                        // Try mounting at the lowest numbered "disk" name we can
                        int n = 1;
                        while( info.mountPath == null )
                        {
                            info.mountPath = computer.mountWritable( n == 1 ? "disk" : "disk" + n, (IWritableMount) this.m_diskMount );
                            n++;
                        }
                    }
                    else
                    {
                        // Try mounting at the lowest numbered "disk" name we can
                        int n = 1;
                        while( info.mountPath == null )
                        {
                            info.mountPath = computer.mount( n == 1 ? "disk" : "disk" + n, this.m_diskMount );
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

    private IMedia getDiskMedia()
    {
        return MediaProviders.get( this.getDiskStack() );
    }

    @Nonnull
    ItemStack getDiskStack()
    {
        return this.getStack( 0 );
    }

    void setDiskStack( @Nonnull ItemStack stack )
    {
        this.setStack( 0, stack );
    }

    void unmount( IComputerAccess computer )
    {
        synchronized( this )
        {
            this.unmountDisk( computer );
            this.m_computers.remove( computer );
        }
    }

    private synchronized void unmountDisk( IComputerAccess computer )
    {
        if( !this.m_diskStack.isEmpty() )
        {
            MountInfo info = this.m_computers.get( computer );
            assert info != null;
            if( info.mountPath != null )
            {
                computer.unmount( info.mountPath );
                info.mountPath = null;
            }
            computer.queueEvent( "disk_eject", computer.getAttachmentName() );
        }
    }

    void playDiskAudio()
    {
        synchronized( this )
        {
            IMedia media = this.getDiskMedia();
            if( media != null && media.getAudioTitle( this.m_diskStack ) != null )
            {
                this.m_recordQueued = true;
                this.m_restartRecord = this.m_recordPlaying;
            }
        }
    }

    void stopDiskAudio()
    {
        synchronized( this )
        {
            this.m_recordQueued = false;
            this.m_restartRecord = false;
        }
    }

    // private methods

    void ejectDisk()
    {
        synchronized( this )
        {
            this.m_ejectQueued = true;
        }
    }

    private void updateBlockState()
    {
        if( this.removed )
        {
            return;
        }

        if( !this.m_diskStack.isEmpty() )
        {
            IMedia contents = this.getDiskMedia();
            this.updateBlockState( contents != null ? DiskDriveState.FULL : DiskDriveState.INVALID );
        }
        else
        {
            this.updateBlockState( DiskDriveState.EMPTY );
        }
    }

    private void updateBlockState( DiskDriveState state )
    {
        BlockState blockState = this.getCachedState();
        if( blockState.get( BlockDiskDrive.STATE ) == state )
        {
            return;
        }

        this.getWorld().setBlockState( this.getPos(), blockState.with( BlockDiskDrive.STATE, state ) );
    }

    private synchronized void ejectContents( boolean destroyed )
    {
        if( this.getWorld().isClient || this.m_diskStack.isEmpty() )
        {
            return;
        }

        // Remove the disks from the inventory
        ItemStack disks = this.m_diskStack;
        this.setDiskStack( ItemStack.EMPTY );

        // Spawn the item in the world
        int xOff = 0;
        int zOff = 0;
        if( !destroyed )
        {
            Direction dir = this.getDirection();
            xOff = dir.getOffsetX();
            zOff = dir.getOffsetZ();
        }

        BlockPos pos = this.getPos();
        double x = pos.getX() + 0.5 + xOff * 0.5;
        double y = pos.getY() + 0.75;
        double z = pos.getZ() + 0.5 + zOff * 0.5;
        ItemEntity entityitem = new ItemEntity( this.getWorld(), x, y, z, disks );
        entityitem.setVelocity( xOff * 0.15, 0, zOff * 0.15 );

        this.getWorld().spawnEntity( entityitem );
        if( !destroyed )
        {
            this.getWorld().syncGlobalEvent( 1000, this.getPos(), 0 );
        }
    }

    private void playRecord()
    {
        IMedia contents = this.getDiskMedia();
        SoundEvent record = contents != null ? contents.getAudio( this.m_diskStack ) : null;
        if( record != null )
        {
            RecordUtil.playRecord( record, contents.getAudioTitle( this.m_diskStack ), this.getWorld(), this.getPos() );
        }
        else
        {
            RecordUtil.playRecord( null, null, this.getWorld(), this.getPos() );
        }
    }

    // Private methods

    private void stopRecord()
    {
        RecordUtil.playRecord( null, null, this.getWorld(), this.getPos() );
    }

    @Nonnull
    @Override
    public Text getName()
    {
        return this.customName != null ? this.customName : new TranslatableText( this.getCachedState().getBlock()
            .getTranslationKey() );
    }

    @Override
    public boolean hasCustomName()
    {
        return this.customName != null;
    }

    @Nonnull
    @Override
    public Text getDisplayName()
    {
        return Nameable.super.getDisplayName();
    }

    @Nullable
    @Override
    public Text getCustomName()
    {
        return this.customName;
    }

    @Nonnull
    @Override
    public ScreenHandler createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        return new ContainerDiskDrive( id, inventory, this );
    }

    private static class MountInfo
    {
        String mountPath;
    }
}
