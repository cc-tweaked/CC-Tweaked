/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.inventory;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerTurtle extends Container implements IContainerComputer
{
    public static final ContainerType<ContainerTurtle> TYPE = null;

    public static final int PLAYER_START_Y = 134;
    public static final int TURTLE_START_X = 175;

    private final ITurtleAccess m_turtle;
    private IComputer m_computer;
    private final InputState input = new InputState( this );
    private int selectedSlot;

    protected ContainerTurtle( int id, PlayerInventory playerInventory, ITurtleAccess turtle )
    {
        super( TYPE, id );

        m_turtle = turtle;
        selectedSlot = m_turtle.getWorld().isRemote ? 0 : m_turtle.getSelectedSlot();

        // Turtle inventory
        for( int y = 0; y < 4; y++ )
        {
            for( int x = 0; x < 4; x++ )
            {
                addSlot( new Slot( m_turtle.getInventory(), x + y * 4, TURTLE_START_X + 1 + x * 18, PLAYER_START_Y + 1 + y * 18 ) );
            }
        }

        // Player inventory
        for( int y = 0; y < 3; y++ )
        {
            for( int x = 0; x < 9; x++ )
            {
                addSlot( new Slot( playerInventory, x + y * 9 + 9, 8 + x * 18, PLAYER_START_Y + 1 + y * 18 ) );
            }
        }

        // Player hotbar
        for( int x = 0; x < 9; x++ )
        {
            addSlot( new Slot( playerInventory, x, 8 + x * 18, PLAYER_START_Y + 3 * 18 + 5 ) );
        }
    }

    public ContainerTurtle( int id, PlayerInventory playerInventory, ITurtleAccess turtle, IComputer computer )
    {
        this( id, playerInventory, turtle );
        m_computer = computer;
    }

    public int getSelectedSlot()
    {
        return selectedSlot;
    }

    @Override
    public boolean canInteractWith( @Nonnull PlayerEntity player )
    {
        TileTurtle turtle = ((TurtleBrain) m_turtle).getOwner();
        return turtle != null && turtle.isUsableByPlayer( player );
    }

    @Nonnull
    private ItemStack tryItemMerge( PlayerEntity player, int slotNum, int firstSlot, int lastSlot, boolean reverse )
    {
        Slot slot = inventorySlots.get( slotNum );
        ItemStack originalStack = ItemStack.EMPTY;
        if( slot != null && slot.getHasStack() )
        {
            ItemStack clickedStack = slot.getStack();
            originalStack = clickedStack.copy();
            if( !mergeItemStack( clickedStack, firstSlot, lastSlot, reverse ) )
            {
                return ItemStack.EMPTY;
            }

            if( clickedStack.isEmpty() )
            {
                slot.putStack( ItemStack.EMPTY );
            }
            else
            {
                slot.onSlotChanged();
            }

            if( clickedStack.getCount() != originalStack.getCount() )
            {
                slot.onTake( player, clickedStack );
            }
            else
            {
                return ItemStack.EMPTY;
            }
        }
        return originalStack;
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot( PlayerEntity player, int slotNum )
    {
        if( slotNum >= 0 && slotNum < 16 )
        {
            return tryItemMerge( player, slotNum, 16, 52, true );
        }
        else if( slotNum >= 16 )
        {
            return tryItemMerge( player, slotNum, 0, 16, false );
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        return m_computer;
    }

    @Nonnull
    @Override
    public InputState getInput()
    {
        return input;
    }

    @Override
    public void onContainerClosed( PlayerEntity player )
    {
        super.onContainerClosed( player );
        input.close();
    }
}
