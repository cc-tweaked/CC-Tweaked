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
import net.minecraft.nbt.NbtCompound;
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
    private final Map<IComputerAccess, MountInfo> computers = new HashMap<>();
    Text customName;
    @Nonnull
    private ItemStack diskStack = ItemStack.EMPTY;
    private IMount diskMount = null;
    private boolean recordQueued = false;
    private boolean recordPlaying = false;
    private boolean restartRecord = false;
    private boolean ejectQueued;

    public TileDiskDrive( BlockEntityType<TileDiskDrive> type )
    {
        super( type );
    }

    @Override
    public void destroy()
    {
        ejectContents( true );
        if( recordPlaying )
        {
            stopRecord();
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
            if( !getWorld().isClient )
            {
                player.openHandledScreen( this );
            }
            return ActionResult.SUCCESS;
        }
    }

    public Direction getDirection()
    {
        return getCachedState().get( BlockDiskDrive.FACING );
    }

    @Override
    public void readNbt( @Nonnull BlockState state, @Nonnull NbtCompound nbt )
    {
        super.readNbt( state, nbt );
        customName = nbt.contains( NBT_NAME ) ? Text.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;
        if( nbt.contains( NBT_ITEM ) )
        {
            NbtCompound item = nbt.getCompound( NBT_ITEM );
            diskStack = ItemStack.fromNbt( item );
            diskMount = null;
        }
    }

    @Nonnull
    @Override
    public NbtCompound writeNbt( @Nonnull NbtCompound nbt )
    {
        if( customName != null )
        {
            nbt.putString( NBT_NAME, Text.Serializer.toJson( customName ) );
        }

        if( !diskStack.isEmpty() )
        {
            NbtCompound item = new NbtCompound();
            diskStack.writeNbt( item );
            nbt.put( NBT_ITEM, item );
        }
        return super.writeNbt( nbt );
    }

    @Override
    public void markDirty()
    {
        if( !world.isClient )
        {
            updateBlockState();
        }
        super.markDirty();
    }

    @Override
    public void tick()
    {
        // Ejection
        if( ejectQueued )
        {
            ejectContents( false );
            ejectQueued = false;
        }

        // Music
        synchronized( this )
        {
            if( !world.isClient && recordPlaying != recordQueued || restartRecord )
            {
                restartRecord = false;
                if( recordQueued )
                {
                    IMedia contents = getDiskMedia();
                    SoundEvent record = contents != null ? contents.getAudio( diskStack ) : null;
                    if( record != null )
                    {
                        recordPlaying = true;
                        playRecord();
                    }
                    else
                    {
                        recordQueued = false;
                    }
                }
                else
                {
                    stopRecord();
                    recordPlaying = false;
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
        return diskStack.isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack getStack( int slot )
    {
        return diskStack;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot, int count )
    {
        if( diskStack.isEmpty() )
        {
            return ItemStack.EMPTY;
        }

        if( diskStack.getCount() <= count )
        {
            ItemStack disk = diskStack;
            setStack( slot, ItemStack.EMPTY );
            return disk;
        }

        ItemStack part = diskStack.split( count );
        setStack( slot, diskStack.isEmpty() ? ItemStack.EMPTY : diskStack );
        return part;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot )
    {
        ItemStack result = diskStack;
        diskStack = ItemStack.EMPTY;
        diskMount = null;

        return result;
    }

    @Override
    public void setStack( int slot, @Nonnull ItemStack stack )
    {
        if( getWorld().isClient )
        {
            diskStack = stack;
            diskMount = null;
            markDirty();
            return;
        }

        synchronized( this )
        {
            if( InventoryUtil.areItemsStackable( stack, diskStack ) )
            {
                diskStack = stack;
                return;
            }

            // Unmount old disk
            if( !diskStack.isEmpty() )
            {
                // TODO: Is this iteration thread safe?
                Set<IComputerAccess> computers = this.computers.keySet();
                for( IComputerAccess computer : computers )
                {
                    unmountDisk( computer );
                }
            }

            // Stop music
            if( recordPlaying )
            {
                stopRecord();
                recordPlaying = false;
                recordQueued = false;
            }

            // Swap disk over
            diskStack = stack;
            diskMount = null;
            markDirty();

            // Mount new disk
            if( !diskStack.isEmpty() )
            {
                Set<IComputerAccess> computers = this.computers.keySet();
                for( IComputerAccess computer : computers )
                {
                    mountDisk( computer );
                }
            }
        }
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
    public IPeripheral getPeripheral( Direction side )
    {
        return new DiskDrivePeripheral( this );
    }

    String getDiskMountPath( IComputerAccess computer )
    {
        synchronized( this )
        {
            MountInfo info = computers.get( computer );
            return info != null ? info.mountPath : null;
        }
    }

    void mount( IComputerAccess computer )
    {
        synchronized( this )
        {
            computers.put( computer, new MountInfo() );
            mountDisk( computer );
        }
    }

    private synchronized void mountDisk( IComputerAccess computer )
    {
        if( !diskStack.isEmpty() )
        {
            MountInfo info = computers.get( computer );
            IMedia contents = getDiskMedia();
            if( contents != null )
            {
                if( diskMount == null )
                {
                    diskMount = contents.createDataMount( diskStack, getWorld() );
                }
                if( diskMount != null )
                {
                    if( diskMount instanceof IWritableMount )
                    {
                        // Try mounting at the lowest numbered "disk" name we can
                        int n = 1;
                        while( info.mountPath == null )
                        {
                            info.mountPath = computer.mountWritable( n == 1 ? "disk" : "disk" + n, (IWritableMount) diskMount );
                            n++;
                        }
                    }
                    else
                    {
                        // Try mounting at the lowest numbered "disk" name we can
                        int n = 1;
                        while( info.mountPath == null )
                        {
                            info.mountPath = computer.mount( n == 1 ? "disk" : "disk" + n, diskMount );
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
        return MediaProviders.get( getDiskStack() );
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

    void unmount( IComputerAccess computer )
    {
        synchronized( this )
        {
            unmountDisk( computer );
            computers.remove( computer );
        }
    }

    private synchronized void unmountDisk( IComputerAccess computer )
    {
        if( !diskStack.isEmpty() )
        {
            MountInfo info = computers.get( computer );
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
            IMedia media = getDiskMedia();
            if( media != null && media.getAudioTitle( diskStack ) != null )
            {
                recordQueued = true;
                restartRecord = recordPlaying;
            }
        }
    }

    void stopDiskAudio()
    {
        synchronized( this )
        {
            recordQueued = false;
            restartRecord = false;
        }
    }

    // private methods

    void ejectDisk()
    {
        synchronized( this )
        {
            ejectQueued = true;
        }
    }

    private void updateBlockState()
    {
        if( removed )
        {
            return;
        }

        if( !diskStack.isEmpty() )
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
        if( blockState.get( BlockDiskDrive.STATE ) == state )
        {
            return;
        }

        getWorld().setBlockState( getPos(), blockState.with( BlockDiskDrive.STATE, state ) );
    }

    private synchronized void ejectContents( boolean destroyed )
    {
        if( getWorld().isClient || diskStack.isEmpty() )
        {
            return;
        }

        // Remove the disks from the inventory
        ItemStack disks = diskStack;
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
        if( !destroyed )
        {
            getWorld().syncGlobalEvent( 1000, getPos(), 0 );
        }
    }

    private void playRecord()
    {
        IMedia contents = getDiskMedia();
        SoundEvent record = contents != null ? contents.getAudio( diskStack ) : null;
        if( record != null )
        {
            RecordUtil.playRecord( record, contents.getAudioTitle( diskStack ), getWorld(), getPos() );
        }
        else
        {
            RecordUtil.playRecord( null, null, getWorld(), getPos() );
        }
    }

    // Private methods

    private void stopRecord()
    {
        RecordUtil.playRecord( null, null, getWorld(), getPos() );
    }

    @Nonnull
    @Override
    public Text getName()
    {
        return customName != null ? customName : new TranslatableText( getCachedState().getBlock()
            .getTranslationKey() );
    }

    @Override
    public boolean hasCustomName()
    {
        return customName != null;
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
        return customName;
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
