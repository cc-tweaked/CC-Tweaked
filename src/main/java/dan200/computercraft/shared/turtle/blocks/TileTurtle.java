/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.blocks;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.computer.blocks.ComputerPeripheral;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.util.DefaultInventory;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.RedstoneUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public class TileTurtle extends TileComputerBase implements ITurtleTile, DefaultInventory
{
    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;

    enum MoveState
    {
        NOT_MOVED,
        IN_PROGRESS,
        MOVED
    }

    private final NonNullList<ItemStack> m_inventory = NonNullList.withSize( INVENTORY_SIZE, ItemStack.EMPTY );
    private final NonNullList<ItemStack> m_previousInventory = NonNullList.withSize( INVENTORY_SIZE, ItemStack.EMPTY );
    private final IItemHandlerModifiable m_itemHandler = new InvWrapper( this );
    private boolean m_inventoryChanged = false;
    private TurtleBrain m_brain = new TurtleBrain( this );
    private MoveState m_moveState = MoveState.NOT_MOVED;
    private ComputerFamily m_family;

    public TileTurtle()
    {
        this( ComputerFamily.Normal );
    }

    public TileTurtle( ComputerFamily family )
    {
        m_family = family;
    }

    private boolean hasMoved()
    {
        return m_moveState == MoveState.MOVED;
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ServerComputer computer = new ServerComputer(
            getWorld(), id, label, instanceID, getFamily(),
            ComputerCraft.terminalWidth_turtle, ComputerCraft.terminalHeight_turtle
        );
        computer.setPosition( getPos() );
        computer.addAPI( new TurtleAPI( computer.getAPIEnvironment(), getAccess() ) );
        m_brain.setupComputer( computer );
        return computer;
    }

    @Override
    public ComputerProxy createProxy()
    {
        return m_brain.getProxy();
    }

    @Override
    public void destroy()
    {
        if( !hasMoved() )
        {
            // Stop computer
            super.destroy();

            // Drop contents
            if( !getWorld().isRemote )
            {
                int size = getSizeInventory();
                for( int i = 0; i < size; i++ )
                {
                    ItemStack stack = getStackInSlot( i );
                    if( !stack.isEmpty() )
                    {
                        WorldUtil.dropItemStack( stack, getWorld(), getPos() );
                    }
                }
            }
        }
        else
        {
            // Just turn off any redstone we had on
            for( EnumFacing dir : EnumFacing.VALUES )
            {
                RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
            }
        }
    }

    @Override
    protected void unload()
    {
        if( !hasMoved() )
        {
            super.unload();
        }
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        // Request description from server
        // requestTileEntityUpdate();

        // Apply dye
        ItemStack currentItem = player.getHeldItem( hand );
        if( !currentItem.isEmpty() )
        {
            if( currentItem.getItem() == Items.DYE )
            {
                // Dye to change turtle colour
                if( !getWorld().isRemote )
                {
                    int dye = currentItem.getItemDamage() & 0xf;
                    if( m_brain.getDyeColour() != dye )
                    {
                        m_brain.setDyeColour( dye );
                        if( !player.capabilities.isCreativeMode )
                        {
                            currentItem.shrink( 1 );
                        }
                    }
                }
                return true;
            }
            else if( currentItem.getItem() == Items.WATER_BUCKET && m_brain.getColour() != -1 )
            {
                // Water to remove turtle colour
                if( !getWorld().isRemote )
                {
                    if( m_brain.getColour() != -1 )
                    {
                        m_brain.setColour( -1 );
                        if( !player.capabilities.isCreativeMode )
                        {
                            player.setHeldItem( hand, new ItemStack( Items.BUCKET ) );
                            player.inventory.markDirty();
                        }
                    }
                }
                return true;
            }
        }

        // Open GUI or whatever
        return super.onActivate( player, hand, side, hitX, hitY, hitZ );
    }

    @Override
    protected boolean canNameWithTag( EntityPlayer player )
    {
        return true;
    }

    @Override
    public void openGUI( EntityPlayer player )
    {
        Containers.openTurtleGUI( player, this );
    }

    @Override
    protected double getInteractRange( EntityPlayer player )
    {
        return 12.0;
    }

    @Override
    public void update()
    {
        super.update();
        m_brain.update();
        if( !getWorld().isRemote && m_inventoryChanged )
        {
            ServerComputer computer = getServerComputer();
            if( computer != null ) computer.queueEvent( "turtle_inventory" );

            m_inventoryChanged = false;
            for( int n = 0; n < getSizeInventory(); n++ )
            {
                m_previousInventory.set( n, getStackInSlot( n ).copy() );
            }
        }
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        if( m_moveState == MoveState.NOT_MOVED ) super.onNeighbourChange( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        if( m_moveState == MoveState.NOT_MOVED ) super.onNeighbourTileEntityChange( neighbour );
    }

    public void notifyMoveStart()
    {
        if( m_moveState == MoveState.NOT_MOVED ) m_moveState = MoveState.IN_PROGRESS;
    }

    public void notifyMoveEnd()
    {
        // MoveState.MOVED is final
        if( m_moveState == MoveState.IN_PROGRESS ) m_moveState = MoveState.NOT_MOVED;
    }

    @Override
    public void readFromNBT( NBTTagCompound nbt )
    {
        super.readFromNBT( nbt );

        // Read inventory
        NBTTagList nbttaglist = nbt.getTagList( "Items", Constants.NBT.TAG_COMPOUND );
        m_inventory.clear();
        m_previousInventory.clear();
        for( int i = 0; i < nbttaglist.tagCount(); i++ )
        {
            NBTTagCompound tag = nbttaglist.getCompoundTagAt( i );
            int slot = tag.getByte( "Slot" ) & 0xff;
            if( slot < getSizeInventory() )
            {
                m_inventory.set( slot, new ItemStack( tag ) );
                m_previousInventory.set( slot, m_inventory.get( slot ).copy() );
            }
        }

        // Read state
        m_brain.readFromNBT( nbt );
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound nbt )
    {
        // Write inventory
        NBTTagList nbttaglist = new NBTTagList();
        for( int i = 0; i < INVENTORY_SIZE; i++ )
        {
            if( !m_inventory.get( i ).isEmpty() )
            {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte( "Slot", (byte) i );
                m_inventory.get( i ).writeToNBT( tag );
                nbttaglist.appendTag( tag );
            }
        }
        nbt.setTag( "Items", nbttaglist );

        // Write brain
        nbt = m_brain.writeToNBT( nbt );

        return super.writeToNBT( nbt );
    }

    @Override
    protected boolean isPeripheralBlockedOnSide( ComputerSide localSide )
    {
        return hasPeripheralUpgradeOnSide( localSide );
    }

    // IDirectionalTile

    @Override
    public EnumFacing getDirection()
    {
        return m_brain.getDirection();
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
        m_brain.setDirection( dir );
    }

    // ITurtleTile

    @Override
    public ITurtleUpgrade getUpgrade( TurtleSide side )
    {
        return m_brain.getUpgrade( side );
    }

    @Override
    public int getColour()
    {
        return m_brain.getColour();
    }

    @Override
    public ResourceLocation getOverlay()
    {
        return m_brain.getOverlay();
    }

    @Override
    public ITurtleAccess getAccess()
    {
        return m_brain;
    }

    @Override
    public Vec3d getRenderOffset( float f )
    {
        return m_brain.getRenderOffset( f );
    }

    @Override
    public float getRenderYaw( float f )
    {
        return m_brain.getVisualYaw( f );
    }

    @Override
    public float getToolRenderAngle( TurtleSide side, float f )
    {
        return m_brain.getToolRenderAngle( side, f );
    }

    // IComputerTile
    @Override
    public ComputerFamily getFamily()
    {
        return m_family;
    }

    void setOwningPlayer( GameProfile player )
    {
        m_brain.setOwningPlayer( player );
        markDirty();
    }

    // IInventory

    @Override
    public int getSizeInventory()
    {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty()
    {
        for( ItemStack stack : m_inventory )
        {
            if( !stack.isEmpty() ) return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot( int slot )
    {
        return slot >= 0 && slot < INVENTORY_SIZE ? m_inventory.get( slot ) : ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack removeStackFromSlot( int slot )
    {
        ItemStack result = getStackInSlot( slot );
        setInventorySlotContents( slot, ItemStack.EMPTY );
        return result;
    }

    @Nonnull
    @Override
    public ItemStack decrStackSize( int slot, int count )
    {
        if( count == 0 ) return ItemStack.EMPTY;

        ItemStack stack = getStackInSlot( slot );
        if( stack.isEmpty() ) return ItemStack.EMPTY;

        if( stack.getCount() <= count )
        {
            setInventorySlotContents( slot, ItemStack.EMPTY );
            return stack;
        }

        ItemStack part = stack.splitStack( count );
        onInventoryDefinitelyChanged();
        return part;
    }

    @Override
    public void setInventorySlotContents( int i, @Nonnull ItemStack stack )
    {
        if( i >= 0 && i < INVENTORY_SIZE && !InventoryUtil.areItemsEqual( stack, m_inventory.get( i ) ) )
        {
            m_inventory.set( i, stack );
            onInventoryDefinitelyChanged();
        }
    }

    @Override
    public void clear()
    {
        boolean changed = false;
        for( int i = 0; i < INVENTORY_SIZE; i++ )
        {
            if( !m_inventory.get( i ).isEmpty() )
            {
                m_inventory.set( i, ItemStack.EMPTY );
                changed = true;
            }
        }

        if( changed ) onInventoryDefinitelyChanged();
    }

    @Override
    public void markDirty()
    {
        super.markDirty();
        if( !m_inventoryChanged )
        {
            for( int n = 0; n < getSizeInventory(); n++ )
            {
                if( !ItemStack.areItemStacksEqual( getStackInSlot( n ), m_previousInventory.get( n ) ) )
                {
                    m_inventoryChanged = true;
                    break;
                }
            }
        }
    }

    @Override
    public boolean isUsableByPlayer( @Nonnull EntityPlayer player )
    {
        return isUsable( player, false );
    }

    private void onInventoryDefinitelyChanged()
    {
        super.markDirty();
        m_inventoryChanged = true;
    }

    public void onTileEntityChange()
    {
        super.markDirty();
    }

    // Networking stuff

    @Override
    protected void writeDescription( @Nonnull NBTTagCompound nbt )
    {
        super.writeDescription( nbt );
        m_brain.writeDescription( nbt );
    }

    @Override
    protected void readDescription( @Nonnull NBTTagCompound nbt )
    {
        super.readDescription( nbt );
        m_brain.readDescription( nbt );
        updateBlock();
    }

    // Privates

    private boolean hasPeripheralUpgradeOnSide( ComputerSide side )
    {
        ITurtleUpgrade upgrade;
        switch( side )
        {
            case RIGHT:
                upgrade = getUpgrade( TurtleSide.Right );
                break;
            case LEFT:
                upgrade = getUpgrade( TurtleSide.Left );
                break;
            default:
                return false;
        }
        return upgrade != null && upgrade.getType().isPeripheral();
    }

    public void transferStateFrom( TileTurtle copy )
    {
        super.transferStateFrom( copy );
        Collections.copy( m_inventory, copy.m_inventory );
        Collections.copy( m_previousInventory, copy.m_previousInventory );
        m_inventoryChanged = copy.m_inventoryChanged;
        m_brain = copy.m_brain;
        m_brain.setOwner( this );
        copy.m_moveState = MoveState.MOVED;
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral( @Nonnull EnumFacing side )
    {
        return hasMoved() ? null : new ComputerPeripheral( "turtle", createProxy() );
    }

    public IItemHandlerModifiable getItemHandler()
    {
        return m_itemHandler;
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
