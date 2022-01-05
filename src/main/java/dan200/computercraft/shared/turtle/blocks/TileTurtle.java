/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.blocks;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

public class TileTurtle extends TileComputerBase
    implements ITurtleTile, DefaultInventory
{
    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;
    private final NonNullList<ItemStack> inventory = NonNullList
        .withSize( INVENTORY_SIZE, ItemStack.EMPTY );
    private final NonNullList<ItemStack> previousInventory = NonNullList
        .withSize( INVENTORY_SIZE, ItemStack.EMPTY );
    private boolean inventoryChanged = false;
    private TurtleBrain brain = new TurtleBrain( this );
    private MoveState moveState = MoveState.NOT_MOVED;

    public TileTurtle( BlockEntityType<? extends TileGeneric> type, BlockPos pos, BlockState state, ComputerFamily family )
    {
        super( type, family, pos, state );
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
    public void destroy()
    {
        if( !hasMoved() )
        {
            // Stop computer
            super.destroy();

            // Drop contents
            if( !getLevel().isClientSide )
            {
                int size = getContainerSize();
                for( int i = 0; i < size; i++ )
                {
                    ItemStack stack = getItem( i );
                    if( !stack.isEmpty() )
                    {
                        WorldUtil.dropItemStack( stack, getLevel(), getBlockPos() );
                    }
                }
            }
        }
        else
        {
            // Just turn off any redstone we had on
            for( Direction dir : DirectionUtil.FACINGS )
            {
                RedstoneUtil.propagateRedstoneOutput( getLevel(), getBlockPos(), dir );
            }
        }
    }

    private boolean hasMoved()
    {
        return moveState == MoveState.MOVED;
    }

    @Override
    public int getContainerSize()
    {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty()
    {
        for( ItemStack stack : inventory )
        {
            if( !stack.isEmpty() )
            {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getItem( int slot )
    {
        return slot >= 0 && slot < INVENTORY_SIZE ? inventory.get( slot )
            : ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack removeItem( int slot, int count )
    {
        if( count == 0 )
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = getItem( slot );
        if( stack.isEmpty() )
        {
            return ItemStack.EMPTY;
        }

        if( stack.getCount() <= count )
        {
            setItem( slot, ItemStack.EMPTY );
            return stack;
        }

        ItemStack part = stack.split( count );
        onInventoryDefinitelyChanged();
        return part;
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate( int slot )
    {
        ItemStack result = getItem( slot );
        setItem( slot, ItemStack.EMPTY );
        return result;
    }

    @Override
    public void setItem( int i, @Nonnull ItemStack stack )
    {
        if( i >= 0 && i < INVENTORY_SIZE
            && !InventoryUtil.areItemsEqual( stack, inventory.get( i ) ) )
        {
            inventory.set( i, stack );
            onInventoryDefinitelyChanged();
        }
    }

    @Override
    public boolean stillValid( @Nonnull Player player )
    {
        return isUsable( player, false );
    }

    private void onInventoryDefinitelyChanged()
    {
        super.setChanged();
        inventoryChanged = true;
    }

    @Override
    protected boolean canNameWithTag( Player player )
    {
        return true;
    }

    @Nonnull
    @Override
    public InteractionResult onActivate( Player player, InteractionHand hand, BlockHitResult hit )
    {
        // Apply dye
        ItemStack currentItem = player.getItemInHand( hand );
        if( !currentItem.isEmpty() )
        {
            if( currentItem.getItem() instanceof DyeItem )
            {
                // Dye to change turtle colour
                if( !getLevel().isClientSide )
                {
                    DyeColor dye = ((DyeItem) currentItem.getItem()).getDyeColor();
                    if( brain.getDyeColour() != dye )
                    {
                        brain.setDyeColour( dye );
                        if( !player.isCreative() )
                        {
                            currentItem.shrink( 1 );
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
            else if( currentItem.getItem() == Items.WATER_BUCKET
                && brain.getColour() != -1 )
            {
                // Water to remove turtle colour
                if( !getLevel().isClientSide )
                {
                    if( brain.getColour() != -1 )
                    {
                        brain.setColour( -1 );
                        if( !player.isCreative() )
                        {
                            player.setItemInHand( hand,
                                new ItemStack( Items.BUCKET ) );
                            player.getInventory().setChanged();
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }

        // Open GUI or whatever
        return super.onActivate( player, hand, hit );
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        if( moveState == MoveState.NOT_MOVED )
        {
            super.onNeighbourChange( neighbour );
        }
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        if( moveState == MoveState.NOT_MOVED )
        {
            super.onNeighbourTileEntityChange( neighbour );
        }
    }

    @Override
    public void serverTick()
    {
        super.serverTick();
        brain.update();
        if( inventoryChanged )
        {
            ServerComputer computer = getServerComputer();
            if( computer != null )
            {
                computer.queueEvent( "turtle_inventory" );
            }

            inventoryChanged = false;
            for( int n = 0; n < getContainerSize(); n++ )
            {
                previousInventory.set( n, getItem( n ).copy() );
            }
        }
    }

    protected void clientTick()
    {
        brain.update();
    }

    @Override
    protected void updateBlockState( ComputerState newState )
    {}

    @Nonnull
    @Override
    public CompoundTag save( @Nonnull CompoundTag nbt )
    {
        // Write inventory
        ListTag nbttaglist = new ListTag();
        for( int i = 0; i < INVENTORY_SIZE; i++ )
        {
            if( !inventory.get( i ).isEmpty() )
            {
                CompoundTag tag = new CompoundTag();
                tag.putByte( "Slot", (byte) i );
                inventory.get( i ).save( tag );
                nbttaglist.add( tag );
            }
        }
        nbt.put( "Items", nbttaglist );

        // Write brain
        nbt = brain.writeToNBT( nbt );

        return super.save( nbt );
    }

    // IDirectionalTile

    @Override
    public void load( @Nonnull CompoundTag nbt )
    {
        super.load( nbt );

        // Read inventory
        ListTag nbttaglist = nbt.getList( "Items", NBTUtil.TAG_COMPOUND );
        inventory.clear();
        previousInventory.clear();
        for( int i = 0; i < nbttaglist.size(); i++ )
        {
            CompoundTag tag = nbttaglist.getCompound( i );
            int slot = tag.getByte( "Slot" ) & 0xff;
            if( slot < getContainerSize() )
            {
                inventory.set( slot, ItemStack.of( tag ) );
                previousInventory.set( slot, inventory.get( slot ).copy() );
            }
        }

        // Read state
        brain.readFromNBT( nbt );
    }

    @Override
    protected boolean isPeripheralBlockedOnSide( ComputerSide localSide )
    {
        return hasPeripheralUpgradeOnSide( localSide );
    }

    // ITurtleTile

    @Override
    public Direction getDirection()
    {
        return getBlockState().getValue( BlockTurtle.FACING );
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ServerComputer computer = new ServerComputer( getLevel(), id, label, instanceID,
            getFamily(), ComputerCraft.turtleTermWidth,
            ComputerCraft.turtleTermHeight );
        computer.setPosition( getBlockPos() );
        computer.addAPI( new TurtleAPI( computer.getAPIEnvironment(), getAccess() ) );
        brain.setupComputer( computer );
        return computer;
    }

    @Override
    protected void writeDescription( @Nonnull CompoundTag nbt )
    {
        super.writeDescription( nbt );
        brain.writeDescription( nbt );
    }

    @Override
    protected void readDescription( @Nonnull CompoundTag nbt )
    {
        super.readDescription( nbt );
        brain.readDescription( nbt );
    }

    @Override
    public ComputerProxy createProxy()
    {
        return brain.getProxy();
    }

    public void setDirection( Direction dir )
    {
        if( dir.getAxis() == Direction.Axis.Y )
        {
            dir = Direction.NORTH;
        }
        level.setBlockAndUpdate( worldPosition, getBlockState().setValue( BlockTurtle.FACING, dir ) );
        updateOutput();
        updateInput();
        onTileEntityChange();
    }

    public void onTileEntityChange()
    {
        super.setChanged();
    }

    private boolean hasPeripheralUpgradeOnSide( ComputerSide side )
    {
        ITurtleUpgrade upgrade;
        switch( side )
        {
            case RIGHT:
                upgrade = getUpgrade( TurtleSide.RIGHT );
                break;
            case LEFT:
                upgrade = getUpgrade( TurtleSide.LEFT );
                break;
            default:
                return false;
        }
        return upgrade != null && upgrade.getType().isPeripheral();
    }

    // IInventory

    @Override
    protected double getInteractRange( Player player )
    {
        return 12.0;
    }

    public void notifyMoveStart()
    {
        if( moveState == MoveState.NOT_MOVED )
        {
            moveState = MoveState.IN_PROGRESS;
        }
    }

    public void notifyMoveEnd()
    {
        // MoveState.MOVED is final
        if( moveState == MoveState.IN_PROGRESS )
        {
            moveState = MoveState.NOT_MOVED;
        }
    }

    @Override
    public int getColour()
    {
        return brain.getColour();
    }

    @Override
    public ResourceLocation getOverlay()
    {
        return brain.getOverlay();
    }

    @Override
    public ITurtleUpgrade getUpgrade( TurtleSide side )
    {
        return brain.getUpgrade( side );
    }

    @Override
    public ITurtleAccess getAccess()
    {
        return brain;
    }

    @Override
    public Vec3 getRenderOffset( float f )
    {
        return brain.getRenderOffset( f );
    }

    @Override
    public float getRenderYaw( float f )
    {
        return brain.getVisualYaw( f );
    }

    @Override
    public float getToolRenderAngle( TurtleSide side, float f )
    {
        return brain.getToolRenderAngle( side, f );
    }

    void setOwningPlayer( GameProfile player )
    {
        brain.setOwningPlayer( player );
        setChanged();
    }

    // Networking stuff

    @Override
    public void setChanged()
    {
        super.setChanged();
        if( !inventoryChanged )
        {
            for( int n = 0; n < getContainerSize(); n++ )
            {
                if( !ItemStack.matches( getItem( n ), previousInventory.get( n ) ) )
                {
                    inventoryChanged = true;
                    break;
                }
            }
        }
    }

    @Override
    public void clearContent()
    {
        boolean changed = false;
        for( int i = 0; i < INVENTORY_SIZE; i++ )
        {
            if( !inventory.get( i ).isEmpty() )
            {
                inventory.set( i, ItemStack.EMPTY );
                changed = true;
            }
        }

        if( changed )
        {
            onInventoryDefinitelyChanged();
        }
    }

    // Privates

    public void transferStateFrom( TileTurtle copy )
    {
        super.transferStateFrom( copy );
        Collections.copy( inventory, copy.inventory );
        Collections.copy( previousInventory, copy.previousInventory );
        inventoryChanged = copy.inventoryChanged;
        brain = copy.brain;
        brain.setOwner( this );

        // Mark the other turtle as having moved, and so its peripheral is dead.
        copy.moveState = MoveState.MOVED;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu( int id, @Nonnull Inventory inventory,
                                             @Nonnull Player player )
    {
        return new ContainerTurtle( id, inventory, brain );
    }

    enum MoveState
    {
        NOT_MOVED, IN_PROGRESS, MOVED
    }
}
