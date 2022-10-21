/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.inventory;

import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.util.SingleIntArray;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

public final class ContainerTurtle extends ContainerComputerBase
{
    public static final int BORDER = 8;
    public static final int PLAYER_START_Y = 134;
    public static final int TURTLE_START_X = ComputerSidebar.WIDTH + 175;
    public static final int PLAYER_START_X = ComputerSidebar.WIDTH + BORDER;

    private final IIntArray data;

    private ContainerTurtle(
        int id, Predicate<PlayerEntity> canUse, ComputerFamily family, @Nullable ServerComputer computer, @Nullable ComputerContainerData menuData,
        PlayerInventory playerInventory, IInventory inventory, IIntArray data
    )
    {
        super( Registry.ModContainers.TURTLE.get(), id, canUse, family, computer, menuData );
        this.data = data;
        addDataSlots( data );

        // Turtle inventory
        for( int y = 0; y < 4; y++ )
        {
            for( int x = 0; x < 4; x++ )
            {
                addSlot( new Slot( inventory, x + y * 4, TURTLE_START_X + 1 + x * 18, PLAYER_START_Y + 1 + y * 18 ) );
            }
        }

        // Player inventory
        for( int y = 0; y < 3; y++ )
        {
            for( int x = 0; x < 9; x++ )
            {
                addSlot( new Slot( playerInventory, x + y * 9 + 9, PLAYER_START_X + x * 18, PLAYER_START_Y + 1 + y * 18 ) );
            }
        }

        // Player hotbar
        for( int x = 0; x < 9; x++ )
        {
            addSlot( new Slot( playerInventory, x, PLAYER_START_X + x * 18, PLAYER_START_Y + 3 * 18 + 5 ) );
        }
    }

    public static ContainerTurtle ofBrain( int id, PlayerInventory player, TurtleBrain turtle )
    {
        return new ContainerTurtle(
            // Laziness in turtle.getOwner() is important here!
            id, p -> turtle.getOwner().stillValid( p ), turtle.getFamily(), turtle.getOwner().createServerComputer(), null,
            player, turtle.getInventory(), (SingleIntArray) turtle::getSelectedSlot
        );
    }

    public static ContainerTurtle ofMenuData( int id, PlayerInventory player, ComputerContainerData data )
    {
        return new ContainerTurtle(
            id, x -> true, data.family(), null, data, player, new Inventory( TileTurtle.INVENTORY_SIZE ), new IntArray( 1 )
        );
    }

    public int getSelectedSlot()
    {
        return data.get( 0 );
    }

    @Nonnull
    private ItemStack tryItemMerge( PlayerEntity player, int slotNum, int firstSlot, int lastSlot, boolean reverse )
    {
        Slot slot = slots.get( slotNum );
        ItemStack originalStack = ItemStack.EMPTY;
        if( slot != null && slot.hasItem() )
        {
            ItemStack clickedStack = slot.getItem();
            originalStack = clickedStack.copy();
            if( !moveItemStackTo( clickedStack, firstSlot, lastSlot, reverse ) )
            {
                return ItemStack.EMPTY;
            }

            if( clickedStack.isEmpty() )
            {
                slot.set( ItemStack.EMPTY );
            }
            else
            {
                slot.setChanged();
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
    public ItemStack quickMoveStack( @Nonnull PlayerEntity player, int slotNum )
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
}
