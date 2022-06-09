/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.util.SingleIntArray;
import dan200.computercraft.shared.util.ValidatingSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;

import javax.annotation.Nonnull;

public class ContainerPrinter extends Container
{
    private final IInventory inventory;
    private final IIntArray properties;

    private ContainerPrinter( int id, PlayerInventory player, IInventory inventory, IIntArray properties )
    {
        super( Registry.ModContainers.PRINTER.get(), id );
        this.properties = properties;
        this.inventory = inventory;

        addDataSlots( properties );

        // Ink slot
        addSlot( new ValidatingSlot( inventory, 0, 13, 35, TilePrinter::isInk ) );

        // In-tray
        for( int x = 0; x < 6; x++ )
        {
            addSlot( new ValidatingSlot( inventory, x + 1, 61 + x * 18, 22, TilePrinter::isPaper ) );
        }

        // Out-tray
        for( int x = 0; x < 6; x++ ) addSlot( new ValidatingSlot( inventory, x + 7, 61 + x * 18, 49, o -> false ) );

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

    public ContainerPrinter( int id, PlayerInventory player )
    {
        this( id, player, new Inventory( TilePrinter.SLOTS ), new IntArray( 1 ) );
    }

    public ContainerPrinter( int id, PlayerInventory player, TilePrinter printer )
    {
        this( id, player, printer, (SingleIntArray) (() -> printer.isPrinting() ? 1 : 0) );
    }

    public boolean isPrinting()
    {
        return properties.get( 0 ) != 0;
    }

    @Override
    public boolean stillValid( @Nonnull PlayerEntity player )
    {
        return inventory.stillValid( player );
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack( @Nonnull PlayerEntity player, int index )
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
