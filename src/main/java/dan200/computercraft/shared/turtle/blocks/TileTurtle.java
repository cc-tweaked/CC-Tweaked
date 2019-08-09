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
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.blocks.ComputerPeripheral;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.util.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileTurtle extends TileComputerBase implements ITurtleTile, DefaultInventory, Nameable
{
    // Statics

    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;

    public static final NamedBlockEntityType<TileTurtle> FACTORY_NORMAL = NamedBlockEntityType.create(
        new Identifier( ComputerCraft.MOD_ID, "turtle_normal" ),
        type -> new TileTurtle( type, ComputerFamily.Normal )
    );

    public static final NamedBlockEntityType<TileTurtle> FACTORY_ADVANCED = NamedBlockEntityType.create(
        new Identifier( ComputerCraft.MOD_ID, "turtle_advanced" ),
        type -> new TileTurtle( type, ComputerFamily.Advanced )
    );

    // Members

    enum MoveState
    {
        NOT_MOVED,
        IN_PROGRESS,
        MOVED
    }

    private DefaultedList<ItemStack> m_inventory;
    private DefaultedList<ItemStack> m_previousInventory;
    private boolean m_inventoryChanged;
    private TurtleBrain m_brain;
    private MoveState m_moveState;

    public TileTurtle( BlockEntityType<? extends TileGeneric> type, ComputerFamily family )
    {
        super( type, family );
        m_inventory = DefaultedList.ofSize( INVENTORY_SIZE, ItemStack.EMPTY );
        m_previousInventory = DefaultedList.ofSize( INVENTORY_SIZE, ItemStack.EMPTY );
        m_inventoryChanged = false;
        m_brain = new TurtleBrain( this );
        m_moveState = MoveState.NOT_MOVED;
    }

    public boolean hasMoved()
    {
        return m_moveState == MoveState.MOVED;
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ServerComputer computer = new ServerComputer(
            getWorld(), id, m_label, instanceID, getFamily(),
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
            if( !getWorld().isClient )
            {
                int size = getInvSize();
                for( int i = 0; i < size; i++ )
                {
                    ItemStack stack = getInvStack( i );
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
            for( Direction dir : DirectionUtil.FACINGS )
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
    public boolean onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        // Apply dye
        ItemStack currentItem = player.getStackInHand( hand );
        if( !currentItem.isEmpty() )
        {
            if( currentItem.getItem() instanceof DyeItem )
            {
                // Dye to change turtle colour
                if( !getWorld().isClient )
                {
                    DyeColor dye = ((DyeItem) currentItem.getItem()).getColor();
                    if( m_brain.getDyeColour() != dye )
                    {
                        m_brain.setDyeColour( dye );
                        if( !player.isCreative() )
                        {
                            currentItem.decrement( 1 );
                        }
                    }
                }
                return true;
            }
            else if( currentItem.getItem() == Items.WATER_BUCKET && m_brain.getColour() != -1 )
            {
                // Water to remove turtle colour
                if( !getWorld().isClient )
                {
                    if( m_brain.getColour() != -1 )
                    {
                        m_brain.setColour( -1 );
                        if( !player.isCreative() )
                        {
                            player.setStackInHand( hand, new ItemStack( Items.BUCKET ) );
                            player.inventory.markDirty();
                        }
                    }
                }
                return true;
            }
        }

        // Open GUI or whatever
        return super.onActivate( player, hand, hit );
    }

    @Override
    protected boolean canNameWithTag( PlayerEntity player )
    {
        return true;
    }

    @Override
    public void openGUI( PlayerEntity player )
    {
        Containers.openTurtleGUI( player, this );
    }

    @Override
    protected double getInteractRange( PlayerEntity player )
    {
        return 12.0;
    }

    @Override
    public void tick()
    {
        super.tick();
        m_brain.update();
        synchronized( m_inventory )
        {
            if( !getWorld().isClient && m_inventoryChanged )
            {
                ServerComputer computer = getServerComputer();
                if( computer != null ) computer.queueEvent( "turtle_inventory" );

                m_inventoryChanged = false;
                for( int n = 0; n < getInvSize(); n++ )
                {
                    m_previousInventory.set( n, InventoryUtil.copyItem( getInvStack( n ) ) );
                }
            }
        }
    }

    @Override
    protected void updateBlockState( ComputerState newState )
    {
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
    public void fromTag( CompoundTag nbt )
    {
        super.fromTag( nbt );

        // Read inventory
        ListTag nbttaglist = nbt.getList( "Items", NBTUtil.TAG_COMPOUND );
        m_inventory = DefaultedList.ofSize( INVENTORY_SIZE, ItemStack.EMPTY );
        m_previousInventory = DefaultedList.ofSize( INVENTORY_SIZE, ItemStack.EMPTY );
        for( int i = 0; i < nbttaglist.size(); i++ )
        {
            CompoundTag tag = nbttaglist.getCompoundTag( i );
            int slot = tag.getByte( "Slot" ) & 0xff;
            if( slot < getInvSize() )
            {
                m_inventory.set( slot, ItemStack.fromTag( tag ) );
                m_previousInventory.set( slot, InventoryUtil.copyItem( m_inventory.get( slot ) ) );
            }
        }

        // Read state
        m_brain.readFromNBT( nbt );
    }

    @Nonnull
    @Override
    public CompoundTag toTag( CompoundTag nbt )
    {
        // Write inventory
        ListTag nbttaglist = new ListTag();
        for( int i = 0; i < INVENTORY_SIZE; i++ )
        {
            if( !m_inventory.get( i ).isEmpty() )
            {
                CompoundTag tag = new CompoundTag();
                tag.putByte( "Slot", (byte) i );
                m_inventory.get( i ).toTag( tag );
                nbttaglist.add( tag );
            }
        }
        nbt.put( "Items", nbttaglist );

        // Write brain
        nbt = m_brain.writeToNBT( nbt );

        return super.toTag( nbt );
    }

    @Override
    protected boolean isPeripheralBlockedOnSide( ComputerSide localSide )
    {
        return hasPeripheralUpgradeOnSide( localSide );
    }

    // IDirectionalTile

    @Override
    public Direction getDirection()
    {
        return getCachedState().get( BlockTurtle.FACING );
    }

    public void setDirection( Direction dir )
    {
        if( dir.getAxis() == Direction.Axis.Y ) dir = Direction.NORTH;
        world.setBlockState( pos, getCachedState().with( BlockTurtle.FACING, dir ) );
        updateOutput();
        updateInput();
        onTileEntityChange();
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
    public Identifier getOverlay()
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

    public void setOwningPlayer( GameProfile player )
    {
        m_brain.setOwningPlayer( player );
        markDirty();
    }

    // IInventory

    @Override
    public int getInvSize()
    {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isInvEmpty()
    {
        for( ItemStack stack : m_inventory )
        {
            if( !stack.isEmpty() ) return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getInvStack( int slot )
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
    public ItemStack removeInvStack( int slot )
    {
        synchronized( m_inventory )
        {
            ItemStack result = getInvStack( slot );
            setInvStack( slot, ItemStack.EMPTY );
            return result;
        }
    }

    @Nonnull
    @Override
    public ItemStack takeInvStack( int slot, int count )
    {
        if( count == 0 )
        {
            return ItemStack.EMPTY;
        }

        synchronized( m_inventory )
        {
            ItemStack stack = getInvStack( slot );
            if( stack.isEmpty() )
            {
                return ItemStack.EMPTY;
            }

            if( stack.getCount() <= count )
            {
                setInvStack( slot, ItemStack.EMPTY );
                return stack;
            }

            ItemStack part = stack.split( count );
            onInventoryDefinitelyChanged();
            return part;
        }
    }

    @Override
    public void setInvStack( int i, @Nonnull ItemStack stack )
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
            for( int i = 0; i < INVENTORY_SIZE; i++ )
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

    @Override
    public void markDirty()
    {
        super.markDirty();
        synchronized( m_inventory )
        {
            if( !m_inventoryChanged )
            {
                for( int n = 0; n < getInvSize(); n++ )
                {
                    if( !ItemStack.areEqualIgnoreDamage( getInvStack( n ), m_previousInventory.get( n ) ) )
                    {
                        m_inventoryChanged = true;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean canPlayerUseInv( @Nonnull PlayerEntity player )
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
    protected void writeDescription( @Nonnull CompoundTag nbt )
    {
        super.writeDescription( nbt );
        m_brain.writeDescription( nbt );
    }

    @Override
    protected void readDescription( @Nonnull CompoundTag nbt )
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
        m_inventory = copy.m_inventory;
        m_previousInventory = copy.m_previousInventory;
        m_inventoryChanged = copy.m_inventoryChanged;
        m_brain = copy.m_brain;
        m_brain.setOwner( this );
        copy.m_moveState = MoveState.MOVED;
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral( @Nonnull Direction side )
    {
        return hasMoved() ? null : new ComputerPeripheral( "turtle", createProxy() );
    }
}
