/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.MediaProviders;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.util.CapabilityUtil;
import dan200.computercraft.shared.util.DefaultInventory;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.RecordUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public final class TileDiskDrive extends TileGeneric implements DefaultInventory, ITickableTileEntity, INameable, INamedContainerProvider
{
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_ITEM = "Item";

    private static class MountInfo
    {
        String mountPath;
    }

    ITextComponent customName;

    private final Map<IComputerAccess, MountInfo> computers = new HashMap<>();

    @Nonnull
    private ItemStack diskStack = ItemStack.EMPTY;
    private LazyOptional<IItemHandlerModifiable> itemHandlerCap;
    private LazyOptional<IPeripheral> peripheralCap;
    private IMount diskMount = null;

    private boolean recordQueued = false;
    private boolean recordPlaying = false;
    private boolean restartRecord = false;
    private boolean ejectQueued;

    public TileDiskDrive( TileEntityType<TileDiskDrive> type )
    {
        super( type );
    }

    @Override
    public void destroy()
    {
        ejectContents( true );
        if( recordPlaying ) stopRecord();
    }

    @Override
    protected void invalidateCaps()
    {
        super.invalidateCaps();
        itemHandlerCap = CapabilityUtil.invalidate( itemHandlerCap );
        peripheralCap = CapabilityUtil.invalidate( peripheralCap );
    }

    @Nonnull
    @Override
    public ActionResultType onActivate( PlayerEntity player, Hand hand, BlockRayTraceResult hit )
    {
        if( player.isCrouching() )
        {
            // Try to put a disk into the drive
            ItemStack disk = player.getItemInHand( hand );
            if( disk.isEmpty() ) return ActionResultType.PASS;
            if( !getLevel().isClientSide && getItem( 0 ).isEmpty() && MediaProviders.get( disk ) != null )
            {
                setDiskStack( disk );
                player.setItemInHand( hand, ItemStack.EMPTY );
            }
            return ActionResultType.SUCCESS;
        }
        else
        {
            // Open the GUI
            if( !getLevel().isClientSide ) NetworkHooks.openGui( (ServerPlayerEntity) player, this );
            return ActionResultType.SUCCESS;
        }
    }

    public Direction getDirection()
    {
        return getBlockState().getValue( BlockDiskDrive.FACING );
    }

    @Override
    public void load( @Nonnull BlockState state, @Nonnull CompoundNBT nbt )
    {
        super.load( state, nbt );
        customName = nbt.contains( NBT_NAME ) ? ITextComponent.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;
        if( nbt.contains( NBT_ITEM ) )
        {
            CompoundNBT item = nbt.getCompound( NBT_ITEM );
            diskStack = ItemStack.of( item );
            diskMount = null;
        }
    }

    @Nonnull
    @Override
    public CompoundNBT save( @Nonnull CompoundNBT nbt )
    {
        if( customName != null ) nbt.putString( NBT_NAME, ITextComponent.Serializer.toJson( customName ) );

        if( !diskStack.isEmpty() )
        {
            CompoundNBT item = new CompoundNBT();
            diskStack.save( item );
            nbt.put( NBT_ITEM, item );
        }
        return super.save( nbt );
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
            if( !level.isClientSide && recordPlaying != recordQueued || restartRecord )
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
    public int getContainerSize()
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
    public ItemStack getItem( int slot )
    {
        return diskStack;
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate( int slot )
    {
        ItemStack result = diskStack;
        diskStack = ItemStack.EMPTY;
        diskMount = null;

        return result;
    }

    @Nonnull
    @Override
    public ItemStack removeItem( int slot, int count )
    {
        if( diskStack.isEmpty() ) return ItemStack.EMPTY;

        if( diskStack.getCount() <= count )
        {
            ItemStack disk = diskStack;
            setItem( slot, ItemStack.EMPTY );
            return disk;
        }

        ItemStack part = diskStack.split( count );
        setItem( slot, diskStack.isEmpty() ? ItemStack.EMPTY : diskStack );
        return part;
    }

    @Override
    public void setItem( int slot, @Nonnull ItemStack stack )
    {
        if( getLevel().isClientSide )
        {
            diskStack = stack;
            diskMount = null;
            setChanged();
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
                for( IComputerAccess computer : computers ) unmountDisk( computer );
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
            setChanged();

            // Mount new disk
            if( !diskStack.isEmpty() )
            {
                Set<IComputerAccess> computers = this.computers.keySet();
                for( IComputerAccess computer : computers ) mountDisk( computer );
            }
        }
    }

    @Override
    public void setChanged()
    {
        if( !level.isClientSide ) updateBlockState();
        super.setChanged();
    }

    @Override
    public boolean stillValid( @Nonnull PlayerEntity player )
    {
        return isUsable( player, false );
    }

    @Override
    public void clearContent()
    {
        setItem( 0, ItemStack.EMPTY );
    }

    @Nonnull
    ItemStack getDiskStack()
    {
        return getItem( 0 );
    }

    void setDiskStack( @Nonnull ItemStack stack )
    {
        setItem( 0, stack );
    }

    private IMedia getDiskMedia()
    {
        return MediaProviders.get( getDiskStack() );
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

    void unmount( IComputerAccess computer )
    {
        synchronized( this )
        {
            unmountDisk( computer );
            computers.remove( computer );
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

    void ejectDisk()
    {
        synchronized( this )
        {
            ejectQueued = true;
        }
    }

    // private methods

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
                    diskMount = contents.createDataMount( diskStack, getLevel() );
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

    private void updateBlockState()
    {
        if( remove || level == null ) return;

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
        BlockState blockState = getBlockState();
        if( blockState.getValue( BlockDiskDrive.STATE ) == state ) return;

        getLevel().setBlockAndUpdate( getBlockPos(), blockState.setValue( BlockDiskDrive.STATE, state ) );
    }

    private synchronized void ejectContents( boolean destroyed )
    {
        if( getLevel().isClientSide || diskStack.isEmpty() ) return;

        // Remove the disks from the inventory
        ItemStack disks = diskStack;
        setDiskStack( ItemStack.EMPTY );

        // Spawn the item in the world
        int xOff = 0;
        int zOff = 0;
        if( !destroyed )
        {
            Direction dir = getDirection();
            xOff = dir.getStepX();
            zOff = dir.getStepZ();
        }

        BlockPos pos = getBlockPos();
        double x = pos.getX() + 0.5 + xOff * 0.5;
        double y = pos.getY() + 0.75;
        double z = pos.getZ() + 0.5 + zOff * 0.5;
        ItemEntity entityitem = new ItemEntity( getLevel(), x, y, z, disks );
        entityitem.setDeltaMovement( xOff * 0.15, 0, zOff * 0.15 );

        getLevel().addFreshEntity( entityitem );
        if( !destroyed ) getLevel().globalLevelEvent( 1000, getBlockPos(), 0 );
    }

    // Private methods

    private void playRecord()
    {
        IMedia contents = getDiskMedia();
        SoundEvent record = contents != null ? contents.getAudio( diskStack ) : null;
        if( record != null )
        {
            RecordUtil.playRecord( record, contents.getAudioTitle( diskStack ), getLevel(), getBlockPos() );
        }
        else
        {
            RecordUtil.playRecord( null, null, getLevel(), getBlockPos() );
        }
    }

    private void stopRecord()
    {
        RecordUtil.playRecord( null, null, getLevel(), getBlockPos() );
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable final Direction side )
    {
        if( cap == ITEM_HANDLER_CAPABILITY )
        {
            if( itemHandlerCap == null ) itemHandlerCap = LazyOptional.of( () -> new InvWrapper( this ) );
            return itemHandlerCap.cast();
        }

        if( cap == CAPABILITY_PERIPHERAL )
        {
            if( peripheralCap == null ) peripheralCap = LazyOptional.of( () -> new DiskDrivePeripheral( this ) );
            return peripheralCap.cast();
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
        return customName != null ? customName : new TranslationTextComponent( getBlockState().getBlock().getDescriptionId() );
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName()
    {
        return INameable.super.getDisplayName();
    }

    @Nonnull
    @Override
    public Container createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        return new ContainerDiskDrive( id, inventory, this );
    }
}
