/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.RedstoneUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public class TileTurtle extends TileComputerBase
    implements ITurtleTile, IInventory
{
    // Statics

    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;

    // Members

    enum MoveState
    {
        NOT_MOVED,
        IN_PROGRESS,
        MOVED
    }

    private NonNullList<ItemStack> m_inventory;
    private NonNullList<ItemStack> m_previousInventory;
    private final IItemHandlerModifiable m_itemHandler = new InvWrapper( this );
    private boolean m_inventoryChanged;
    private TurtleBrain m_brain;
    private MoveState m_moveState;

    public TileTurtle()
    {
        m_inventory = NonNullList.withSize( INVENTORY_SIZE, ItemStack.EMPTY );
        m_previousInventory = NonNullList.withSize( INVENTORY_SIZE, ItemStack.EMPTY );
        m_inventoryChanged = false;
        m_brain = createBrain();
        m_moveState = MoveState.NOT_MOVED;
    }

    public boolean hasMoved()
    {
        return m_moveState == MoveState.MOVED;
    }

    protected TurtleBrain createBrain()
    {
        return new TurtleBrain( this );
    }

    protected final ServerComputer createComputer( int instanceID, int id, int termWidth, int termHeight )
    {
        ServerComputer computer = new ServerComputer(
            getWorld(),
            id,
            m_label,
            instanceID,
            getFamily(),
            termWidth,
            termHeight
        );
        computer.setPosition( getPos() );
        computer.addAPI( new TurtleAPI( computer.getAPIEnvironment(), getAccess() ) );
        m_brain.setupComputer( computer );
        return computer;
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        return createComputer( instanceID, id, ComputerCraft.terminalWidth_turtle, ComputerCraft.terminalHeight_turtle );
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
                for( int i=0; i<size; ++i )
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
    public void getDroppedItems( @Nonnull NonNullList<ItemStack> drops, boolean creative )
    {
        IComputer computer = getComputer();
        if( !creative || (computer != null && computer.getLabel() != null) )
        {
            drops.add( TurtleItemFactory.create( this ) );
        }
    }

    @Override
    public ItemStack getPickedItem()
    {
        return TurtleItemFactory.create( this );
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        // Request description from server
        requestTileEntityUpdate();

        // Apply dye
        ItemStack currentItem = player.getHeldItem( EnumHand.MAIN_HAND );
        if( !currentItem.isEmpty() )
        {
            if( currentItem.getItem() == Items.DYE )
            {
                // Dye to change turtle colour
                if( !getWorld().isRemote )
                {
                    int dye = (currentItem.getItemDamage() & 0xf);
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
                            player.setHeldItem( EnumHand.MAIN_HAND, new ItemStack( Items.BUCKET ) );
                            player.inventory.markDirty();
                        }
                    }
                }
                return true;
            }
        }

        // Open GUI or whatever
        return super.onActivate( player, side, hitX, hitY, hitZ );
    }

    @Override
    protected boolean canNameWithTag( EntityPlayer player )
    {
        return true;
    }

    @Override
    public void openGUI( EntityPlayer player )
    {
        ComputerCraft.openTurtleGUI( player, this );
    }

    @Override
    public boolean isSolidOnSide( int side )
    {
        return false;
    }

    @Override
    public boolean isImmuneToExplosion( Entity exploder )
    {
        if( getFamily() == ComputerFamily.Advanced )
        {
            return true;
        }
        else
        {
            return exploder != null && (exploder instanceof EntityLivingBase || exploder instanceof EntityFireball);
        }
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBounds()
    {
        Vec3d offset = getRenderOffset( 1.0f );
        return new AxisAlignedBB(
            offset.x + 0.125, offset.y + 0.125, offset.z + 0.125,
            offset.x + 0.875, offset.y + 0.875, offset.z + 0.875
        );
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
        synchronized( m_inventory )
        {
            if( !getWorld().isRemote && m_inventoryChanged )
            {
                IComputer computer = getComputer();
                if( computer != null )
                {
                    computer.queueEvent( "turtle_inventory" );
                }

                m_inventoryChanged = false;
                for( int n=0; n<getSizeInventory(); ++n )
                {
                    m_previousInventory.set( n, InventoryUtil.copyItem( getStackInSlot( n ) ) );
                }
            }
        }
    }

    @Override
    public void onNeighbourChange()
    {
        if ( m_moveState == MoveState.NOT_MOVED )
        {
            super.onNeighbourChange();
        }
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour)
    {
        if ( m_moveState == MoveState.NOT_MOVED )
        {
            super.onNeighbourTileEntityChange( neighbour );
        }
    }

    public void notifyMoveStart()
    {
        if (m_moveState == MoveState.NOT_MOVED)
        {
            m_moveState = MoveState.IN_PROGRESS;
        }
    }

    public void notifyMoveEnd()
    {
        // MoveState.MOVED is final
        if (m_moveState == MoveState.IN_PROGRESS)
        {
            m_moveState = MoveState.NOT_MOVED;
        }
    }

    @Override
    public void readFromNBT( NBTTagCompound nbttagcompound )
    {
        super.readFromNBT(nbttagcompound);

        // Read inventory
        NBTTagList nbttaglist = nbttagcompound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        m_inventory = NonNullList.withSize( INVENTORY_SIZE, ItemStack.EMPTY );
        m_previousInventory = NonNullList.withSize( INVENTORY_SIZE, ItemStack.EMPTY );
        for( int i=0; i<nbttaglist.tagCount(); ++i )
        {
            NBTTagCompound itemtag = nbttaglist.getCompoundTagAt( i );
            int slot = itemtag.getByte("Slot") & 0xff;
            if( slot >= 0 && slot < getSizeInventory() )
            {
                m_inventory.set( slot, new ItemStack( itemtag ) );
                m_previousInventory.set( slot, InventoryUtil.copyItem( m_inventory.get( slot ) ) );
            }
        }

        // Read state
        m_brain.readFromNBT( nbttagcompound );
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound nbttagcompound )
    {
        nbttagcompound = super.writeToNBT( nbttagcompound );

        // Write inventory
        NBTTagList nbttaglist = new NBTTagList();
        for( int i=0; i<INVENTORY_SIZE; ++i )
        {
            if( !m_inventory.get(i).isEmpty() )
            {
                NBTTagCompound itemtag = new NBTTagCompound();
                itemtag.setByte( "Slot", (byte)i );
                m_inventory.get(i).writeToNBT(itemtag);
                nbttaglist.appendTag(itemtag);
            }
        }
        nbttagcompound.setTag( "Items", nbttaglist );

        // Write brain
        nbttagcompound = m_brain.writeToNBT( nbttagcompound );

        return nbttagcompound;
    }

    @Override
    protected boolean isPeripheralBlockedOnSide( int localSide )
    {
        return hasPeripheralUpgradeOnSide( localSide );
    }

    @Override
    protected boolean isRedstoneBlockedOnSide( int localSide )
    {
        return false;
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
        if( slot >= 0 && slot < INVENTORY_SIZE )
        {
            synchronized( m_inventory )
            {
                return m_inventory.get( slot );
            }
        }
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack removeStackFromSlot( int slot )
    {
        synchronized( m_inventory )
        {
            ItemStack result = getStackInSlot( slot );
            setInventorySlotContents( slot, ItemStack.EMPTY );
            return result;
        }
    }

    @Nonnull
    @Override
    public ItemStack decrStackSize( int slot, int count )
    {
        if( count == 0 )
        {
            return ItemStack.EMPTY;
        }

        synchronized( m_inventory )
        {
            ItemStack stack = getStackInSlot( slot );
            if( stack.isEmpty() )
            {
                return ItemStack.EMPTY;
            }

            if( stack.getCount() <= count )
            {
                setInventorySlotContents( slot, ItemStack.EMPTY );
                return stack;
            }

            ItemStack part = stack.splitStack( count );
            onInventoryDefinitelyChanged();
            return part;
        }
    }

    @Override
    public void setInventorySlotContents( int i, @Nonnull ItemStack stack )
    {
        if( i >= 0 && i < INVENTORY_SIZE )
        {
            synchronized( m_inventory )
            {
                if( !InventoryUtil.areItemsEqual( stack, m_inventory.get( i ) ) )
                {
                    m_inventory.set( i, stack );
                    onInventoryDefinitelyChanged();
                }
            }
        }
    }

    @Override
    public void clear()
    {
        synchronized( m_inventory )
        {
            boolean changed = false;
            for( int i = 0; i < INVENTORY_SIZE; ++i )
            {
                if( !m_inventory.get( i ).isEmpty() )
                {
                    m_inventory.set( i, ItemStack.EMPTY );
                    changed = true;
                }
            }
            if( changed )
            {
                onInventoryDefinitelyChanged();
            }
        }
    }

    @Nonnull
    @Override
    public String getName()
    {
        IComputer computer = getComputer();
        if( computer != null )
        {
            String label = computer.getLabel();
            if( label != null && label.length() > 0 )
            {
                return label;
            }
        }
        return "tile.computercraft:turtle.name";
    }

    @Override
    public boolean hasCustomName()
    {
        IComputer computer = getComputer();
        if( computer != null )
        {
            String label = computer.getLabel();
            if( label != null && label.length() > 0 )
            {
                return true;
            }
        }
        return false;
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
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public void openInventory( @Nonnull EntityPlayer player )
    {
    }

    @Override
    public void closeInventory( @Nonnull EntityPlayer player )
    {
    }

    @Override
    public boolean isItemValidForSlot( int slot, @Nonnull ItemStack stack )
    {
        return true;
    }

    @Override
    public void markDirty()
    {
        super.markDirty();
        synchronized( m_inventory )
        {
            if( !m_inventoryChanged )
            {
                for( int n=0; n<getSizeInventory(); ++n )
                {
                    if( !ItemStack.areItemStacksEqual( getStackInSlot( n ), m_previousInventory.get( n ) ) )
                    {
                        m_inventoryChanged = true;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isUsableByPlayer( @Nonnull EntityPlayer player )
    {
        return isUsable( player, false );
    }

    @Override
    public int getFieldCount()
    {
        return 0;
    }

    @Override
    public int getField(int id)
    {
        return 0;
    }

    @Override
    public void setField(int id, int value)
    {
    }

    public boolean isUseableByRemote( EntityPlayer player )
    {
        return isUsable( player, true );
    }

    public void onInventoryDefinitelyChanged()
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
    public void writeDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.writeDescription( nbttagcompound );
        m_brain.writeDescription( nbttagcompound );
    }

    @Override
    public void readDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.readDescription( nbttagcompound );
        m_brain.readDescription( nbttagcompound );
        updateBlock();
    }

    // Privates

    private boolean hasPeripheralUpgradeOnSide( int side )
    {
        ITurtleUpgrade upgrade;
        switch( side )
        {
            case 4:    upgrade = getUpgrade( TurtleSide.Right ); break;
            case 5:    upgrade = getUpgrade( TurtleSide.Left ); break;
            default: return false;
        }
        return upgrade != null && upgrade.getType().isPeripheral();
    }

    public void transferStateFrom( TileTurtle copy )
    {
        super.transferStateFrom( copy );
        m_inventory = copy.m_inventory;
        m_previousInventory = copy.m_previousInventory;
        m_inventoryChanged = copy.m_inventoryChanged;
        m_brain = copy.m_brain;
        m_brain.setOwner( this );
        copy.m_moveState = MoveState.MOVED;
    }

    public IItemHandlerModifiable getItemHandler()
    {
        return m_itemHandler;
    }

    @Override
    public boolean hasCapability( @Nonnull Capability<?> capability, @Nullable EnumFacing facing )
    {
        return capability == ITEM_HANDLER_CAPABILITY ||  super.hasCapability( capability, facing );
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
