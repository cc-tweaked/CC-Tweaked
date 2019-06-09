/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.shared.util.SingleIntArray;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;

import javax.annotation.Nonnull;

public class ContainerPrinter extends Container
{
    public static final ContainerType<ContainerPrinter> TYPE = new ContainerType<>( ContainerPrinter::new );

    private final IInventory inventory;
    private final IIntArray properties;

    private ContainerPrinter( int id, PlayerInventory player, IInventory inventory, IIntArray properties )
    {
        super( TYPE, id );
        this.properties = properties;
        this.inventory = inventory;

        func_216961_a( properties );

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

    private ContainerPrinter( int id, PlayerInventory player )
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
    public boolean canInteractWith( @Nonnull PlayerEntity player )
    {
        return inventory.isUsableByPlayer( player );
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot( PlayerEntity player, int index )
    {
        Slot slot = inventorySlots.get( index );
        if( slot == null || !slot.getHasStack() ) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack result = stack.copy();
        if( index < 13 )
        {
            // Transfer from printer to inventory
            if( !mergeItemStack( stack, 13, 49, true ) ) return ItemStack.EMPTY;
        }
        else
        {
            // Transfer from inventory to printer
            if( stack.getItem() instanceof DyeItem )
            {
                if( !mergeItemStack( stack, 0, 1, false ) ) return ItemStack.EMPTY;
            }
            else //if is paper
            {
                if( !mergeItemStack( stack, 1, 13, false ) ) return ItemStack.EMPTY;
            }
        }

        if( stack.isEmpty() )
        {
            slot.putStack( ItemStack.EMPTY );
        }
        else
        {
            slot.onSlotChanged();
        }

        if( stack.getCount() == result.getCount() ) return ItemStack.EMPTY;

        slot.onTake( player, stack );
        return result;
    }
}
