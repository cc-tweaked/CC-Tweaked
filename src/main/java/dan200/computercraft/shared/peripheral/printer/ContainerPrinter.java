/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.util.SingleIntArray;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerPrinter extends AbstractContainerMenu
{
    private final Container inventory;
    private final ContainerData properties;

    private ContainerPrinter( int id, Inventory player, Container inventory, ContainerData properties )
    {
        super( Registry.ModContainers.PRINTER.get(), id );
        this.properties = properties;
        this.inventory = inventory;

        addDataSlots( properties );

        // Ink slot
        addSlot( new Slot( inventory, 0, 13, 35 ) );

        // In-tray
        for( int x = 0; x < 6; x++ ) addSlot( new Slot( inventory, x + 1, 61 + x * 18, 22 ) );

        // Out-tray
        for( int x = 0; x < 6; x++ ) addSlot( new Slot( inventory, x + 7, 61 + x * 18, 49 ) );

        // Player inv
        for( int y = 0; y < 3; y++ )
        {
            for( int x = 0; x < 9; x++ )
            {
                addSlot( new Slot( player, x + y * 9 + 9, 8 + x * 18, 84 + y * 18 ) );
            }
        }

        // Player hotbar
        for( int x = 0; x < 9; x++ )
        {
            addSlot( new Slot( player, x, 8 + x * 18, 142 ) );
        }
    }

    public ContainerPrinter( int id, Inventory player )
    {
        this( id, player, new SimpleContainer( TilePrinter.SLOTS ), new SimpleContainerData( 1 ) );
    }

    public ContainerPrinter( int id, Inventory player, TilePrinter printer )
    {
        this( id, player, printer, (SingleIntArray) (() -> printer.isPrinting() ? 1 : 0) );
    }

    public boolean isPrinting()
    {
        return properties.get( 0 ) != 0;
    }

    @Override
    public boolean stillValid( @Nonnull Player player )
    {
        return inventory.stillValid( player );
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack( @Nonnull Player player, int index )
    {
        Slot slot = slots.get( index );
        if( slot == null || !slot.hasItem() ) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if( index < 13 )
        {
            // Transfer from printer to inventory
            if( !moveItemStackTo( stack, 13, 49, true ) ) return ItemStack.EMPTY;
        }
        else
        {
            // Transfer from inventory to printer
            if( TilePrinter.isInk( stack ) )
            {
                if( !moveItemStackTo( stack, 0, 1, false ) ) return ItemStack.EMPTY;
            }
            else //if is paper
            {
                if( !moveItemStackTo( stack, 1, 13, false ) ) return ItemStack.EMPTY;
            }
        }

        if( stack.isEmpty() )
        {
            slot.set( ItemStack.EMPTY );
        }
        else
        {
            slot.setChanged();
        }

        if( stack.getCount() == result.getCount() ) return ItemStack.EMPTY;

        slot.onTake( player, stack );
        return result;
    }
}
