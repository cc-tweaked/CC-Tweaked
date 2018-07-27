/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.IPeripheralTile;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import dan200.computercraft.shared.util.IDefaultInventory;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public class TileDiskDrive extends TileGeneric implements IDefaultInventory, IPeripheralTile, ITickable
{
    private static class MountInfo
    {
        public String mountPath;
    }

    private final Map<IComputerAccess, MountInfo> m_computers = new HashMap<>();

    @Nonnull
    private DiskDriveState state = DiskDriveState.EMPTY;
    private String label = null;
    private boolean changed = false;

    @Nonnull
    private ItemStack m_diskStack = ItemStack.EMPTY;
    private final IItemHandlerModifiable m_itemHandler = new InvWrapper( this );
    private IMount m_diskMount = null;

    private boolean m_recordQueued = false;
    private boolean m_recordPlaying = false;
    private boolean m_restartRecord = false;
    private boolean m_ejectQueued = false;

    @Nonnull
    @Override
    public ItemStack getPickedItem()
    {
        return PeripheralItemFactory.create( this );
    }

    @Override
    public void destroy()
    {
        ejectContents( true );
        synchronized( this )
        {
            if( m_recordPlaying )
            {
                stopRecord();
            }
        }
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        if( player.isSneaking() )
        {
            // Try to put a disk into the drive
            if( !getWorld().isRemote )
            {
                ItemStack disk = player.getHeldItem( EnumHand.MAIN_HAND );
                if( !disk.isEmpty() && getStackInSlot( 0 ).isEmpty() )
                {
                    if( ComputerCraft.getMedia( disk ) != null )
                    {
                        setInventorySlotContents( 0, disk );
                        player.setHeldItem( EnumHand.MAIN_HAND, ItemStack.EMPTY );
                        return true;
                    }
                }
            }
        }
        else
        {
            // Open the GUI
            if( !getWorld().isRemote )
            {
                ComputerCraft.openDiskDriveGUI( player, this );
            }
            return true;
        }
        return false;
    }

    @Nonnull
    public DiskDriveState getState()
    {
        return state;
    }

    public void setState( @Nonnull DiskDriveState newState )
    {
        if( state != newState )
        {
            state = newState;
            changed = true;
        }
    }

    @Override
    public EnumFacing getDirection()
    {
        return getBlockState().getValue( BlockDiskDrive.FACING );
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
        if( dir.getAxis() == EnumFacing.Axis.Y )
        {
            dir = EnumFacing.NORTH;
        }
        setBlockState( getBlockState().withProperty( BlockDiskDrive.FACING, dir ) );
    }

    @Override
    public void readFromNBT( NBTTagCompound tag )
    {
        super.readFromNBT( tag );
        if( tag.hasKey( "item" ) )
        {
            m_diskStack = new ItemStack( tag.getCompoundTag( "item" ) );
            m_diskMount = null;
        }

        label = tag.hasKey( "label" ) ? tag.getString( "label" ) : null;
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound tag )
    {
        tag = super.writeToNBT( tag );
        if( !m_diskStack.isEmpty() ) tag.setTag( "item", m_diskStack.writeToNBT( new NBTTagCompound() ) );
        if( label != null ) tag.setString( "label", label );
        return tag;
    }

    @Override
    public void update()
    {
        // Ejection
        synchronized( this )
        {
            if( m_ejectQueued )
            {
                ejectContents( false );
                m_ejectQueued = false;
            }
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
                    SoundEvent record = (contents != null) ? contents.getAudio( m_diskStack ) : null;
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

        if( changed )
        {
            updateBlock();
            changed = false;
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
    public ItemStack getStackInSlot( int i )
    {
        return m_diskStack;
    }

    @Nonnull
    @Override
    public ItemStack removeStackFromSlot( int i )
    {
        ItemStack result = m_diskStack;
        m_diskStack = ItemStack.EMPTY;
        m_diskMount = null;

        return result;
    }

    @Nonnull
    @Override
    public ItemStack decrStackSize( int i, int j )
    {
        if( m_diskStack.isEmpty() )
        {
            return ItemStack.EMPTY;
        }

        if( m_diskStack.getCount() <= j )
        {
            ItemStack disk = m_diskStack;
            setInventorySlotContents( 0, ItemStack.EMPTY );
            return disk;
        }

        ItemStack part = m_diskStack.splitStack( j );
        if( m_diskStack.isEmpty() )
        {
            setInventorySlotContents( 0, ItemStack.EMPTY );
        }
        else
        {
            setInventorySlotContents( 0, m_diskStack );
        }
        return part;
    }

    @Override
    public void setInventorySlotContents( int i, @Nonnull ItemStack itemStack )
    {
        if( getWorld().isRemote )
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
                Set<IComputerAccess> computers = m_computers.keySet();
                for( IComputerAccess computer : computers )
                {
                    unmountDisk( computer );
                }
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

            // Update contents
            updateAnim();

            // Mount new disk
            if( !m_diskStack.isEmpty() )
            {
                Set<IComputerAccess> computers = m_computers.keySet();
                for( IComputerAccess computer : computers )
                {
                    mountDisk( computer );
                }
            }
        }
    }

    @Override
    public boolean hasCustomName()
    {
        return getLabel() != null;
    }

    @Nonnull
    @Override
    public String getName()
    {
        String label = getLabel();
        if( label != null )
        {
            return label;
        }
        else
        {
            return "tile.computercraft:drive.name";
        }
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName()
    {
        if( hasCustomName() )
        {
            return new TextComponentString( getName() );
        }
        else
        {
            return new TextComponentTranslation( getName() );
        }
    }

    @Override
    public boolean isUsableByPlayer( @Nonnull EntityPlayer player )
    {
        return isUsable( player, false );
    }

    @Override
    public void clear()
    {
        synchronized( this )
        {
            setInventorySlotContents( 0, ItemStack.EMPTY );
        }
    }

    // IPeripheralTile implementation

    @Override
    public PeripheralType getPeripheralType()
    {
        return PeripheralType.DiskDrive;
    }

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return new DiskDrivePeripheral( this );
    }

    @Override
    public String getLabel()
    {
        return label;
    }

    public void setLabel( String label )
    {
        this.label = label;
    }

    @Nonnull
    public ItemStack getDiskStack()
    {
        synchronized( this )
        {
            return getStackInSlot( 0 );
        }
    }

    public void setDiskStack( @Nonnull ItemStack stack )
    {
        synchronized( this )
        {
            setInventorySlotContents( 0, stack );
        }
    }

    public IMedia getDiskMedia()
    {
        return ComputerCraft.getMedia( getDiskStack() );
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
            if( !m_ejectQueued )
            {
                m_ejectQueued = true;
            }
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
                            info.mountPath = computer.mountWritable( (n == 1) ? "disk" : ("disk" + n), (IWritableMount) m_diskMount );
                            n++;
                        }
                    }
                    else
                    {
                        // Try mounting at the lowest numbered "disk" name we can
                        int n = 1;
                        while( info.mountPath == null )
                        {
                            info.mountPath = computer.mount( (n == 1) ? "disk" : ("disk" + n), m_diskMount );
                            n++;
                        }
                    }
                }
                else
                {
                    info.mountPath = null;
                }
            }
            computer.queueEvent( "disk", new Object[]{ computer.getAttachmentName() } );
        }
    }

    private synchronized void unmountDisk( IComputerAccess computer )
    {
        if( !m_diskStack.isEmpty() )
        {
            MountInfo info = m_computers.get( computer );
            assert (info != null);
            if( info.mountPath != null )
            {
                computer.unmount( info.mountPath );
                info.mountPath = null;
            }
            computer.queueEvent( "disk_eject", new Object[]{ computer.getAttachmentName() } );
        }
    }

    private synchronized void updateAnim()
    {
        if( !m_diskStack.isEmpty() )
        {
            setState( getDiskMedia() != null ? DiskDriveState.FULL : DiskDriveState.INVALID );
        }
        else
        {
            setState( DiskDriveState.EMPTY );
        }
    }

    private synchronized void ejectContents( boolean destroyed )
    {
        if( getWorld().isRemote ) return;

        if( !m_diskStack.isEmpty() )
        {
            // Remove the disks from the inventory
            ItemStack disks = m_diskStack;
            setInventorySlotContents( 0, ItemStack.EMPTY );

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
            double x = pos.getX() + 0.5 + (xOff * 0.5);
            double y = pos.getY() + 0.75;
            double z = pos.getZ() + 0.5 + (zOff * 0.5);
            EntityItem entityitem = new EntityItem( getWorld(), x, y, z, disks );
            entityitem.motionX = xOff * 0.15;
            entityitem.motionY = 0.0;
            entityitem.motionZ = zOff * 0.15;

            getWorld().spawnEntity( entityitem );
            if( !destroyed )
            {
                getWorld().playBroadcastSound( 1000, getPos(), 0 );
            }
        }
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound tag )
    {
        super.readDescription( tag );
        m_diskStack = tag.hasKey( "item" ) ? new ItemStack( tag.getCompoundTag( "item" ) ) : ItemStack.EMPTY;
        label = tag.hasKey( "label" ) ? tag.getString( "label" ) : null;
        state = DiskDriveState.of( tag.getByte( "state" ) );
        updateBlock();
    }

    @Override
    public void writeDescription( @Nonnull NBTTagCompound tag )
    {
        super.writeDescription( tag );
        if( !m_diskStack.isEmpty() ) tag.setTag( "item", m_diskStack.writeToNBT( new NBTTagCompound() ) );
        if( label != null ) tag.setString( "label", label );
        tag.setByte( "state", (byte) state.ordinal() );
    }

    // Private methods

    private void playRecord()
    {
        IMedia contents = getDiskMedia();
        SoundEvent record = (contents != null) ? contents.getAudio( m_diskStack ) : null;
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

    @Override
    public boolean hasCapability( @Nonnull Capability<?> capability, @Nullable EnumFacing facing )
    {
        return capability == ITEM_HANDLER_CAPABILITY || super.hasCapability( capability, facing );
    }

    @Nullable
    @Override
    public <T> T getCapability( @Nonnull Capability<T> capability, @Nullable EnumFacing facing )
    {
        if( capability == ITEM_HANDLER_CAPABILITY )
        {
            return ITEM_HANDLER_CAPABILITY.cast( m_itemHandler );
        }
        return super.getCapability( capability, facing );
    }
}
