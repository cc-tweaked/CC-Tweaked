/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.inventory;

import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.util.SingleIntArray;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public class ContainerTurtle extends ContainerComputerBase
{
    public static final int BORDER = 8;
    public static final int PLAYER_START_Y = 134;
    public static final int TURTLE_START_X = ComputerSidebar.WIDTH + 175;
    public static final int PLAYER_START_X = ComputerSidebar.WIDTH + BORDER;

    private final ContainerData properties;

    private ContainerTurtle(
        int id, Predicate<Player> canUse, IComputer computer, ComputerFamily family,
        Inventory playerInventory, Container inventory, ContainerData properties
    )
    {
        super( Registry.ModContainers.TURTLE.get(), id, canUse, computer, family );
        this.properties = properties;

        addDataSlots( properties );

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

    public ContainerTurtle( int id, Inventory player, TurtleBrain turtle )
    {
        this(
            id, p -> turtle.getOwner().stillValid( p ), turtle.getOwner().createServerComputer(), turtle.getFamily(),
            player, turtle.getInventory(), (SingleIntArray) turtle::getSelectedSlot
        );
    }

    public ContainerTurtle( int id, Inventory player, ComputerContainerData data )
    {
        this(
            id, x -> true, getComputer( player, data ), data.getFamily(),
            player, new SimpleContainer( TileTurtle.INVENTORY_SIZE ), new SimpleContainerData( 1 )
        );
    }

    public int getSelectedSlot()
    {
        return properties.get( 0 );
    }

    @Nonnull
    private ItemStack tryItemMerge( Player player, int slotNum, int firstSlot, int lastSlot, boolean reverse )
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
    public ItemStack quickMoveStack( @Nonnull Player player, int slotNum )
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
