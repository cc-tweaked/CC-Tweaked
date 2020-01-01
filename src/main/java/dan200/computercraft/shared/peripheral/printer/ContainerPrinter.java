/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.printer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerPrinter extends Container
{
    private static final int PROPERTY_PRINTING = 0;

    private final TilePrinter m_printer;
    private boolean m_lastPrinting;

    public ContainerPrinter( IInventory playerInventory, TilePrinter printer )
    {
        m_printer = printer;
        m_lastPrinting = false;

        // Ink slot
        addSlotToContainer( new Slot( m_printer, 0, 13, 35 ) );

        // In-tray
        for( int x = 0; x < 6; x++ ) addSlotToContainer( new Slot( m_printer, x + 1, 61 + x * 18, 22 ) );

        // Out-tray
        for( int x = 0; x < 6; x++ ) addSlotToContainer( new Slot( m_printer, x + 7, 61 + x * 18, 49 ) );

        // Player inv
        for( int y = 0; y < 3; y++ )
        {
            for( int x = 0; x < 9; x++ )
            {
                addSlotToContainer( new Slot( playerInventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18 ) );
            }
        }

        // Player hotbar
        for( int x = 0; x < 9; x++ ) addSlotToContainer( new Slot( playerInventory, x, 8 + x * 18, 142 ) );
    }

    public boolean isPrinting()
    {
        return m_lastPrinting;
    }

    public TilePrinter getPrinter()
    {
        return m_printer;
    }

    @Override
    public void addListener( IContainerListener listener )
    {
        super.addListener( listener );
        listener.sendWindowProperty( this, PROPERTY_PRINTING, m_printer.isPrinting() ? 1 : 0 );
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        if( !m_printer.getWorld().isRemote )
        {
            boolean printing = m_printer.isPrinting();
            if( printing != m_lastPrinting )
            {
                for( IContainerListener listener : listeners )
                {
                    listener.sendWindowProperty( this, PROPERTY_PRINTING, printing ? 1 : 0 );
                }
                m_lastPrinting = printing;
            }
        }
    }

    @Override
    public void updateProgressBar( int property, int value )
    {
        if( m_printer.getWorld().isRemote )
        {
            if( property == PROPERTY_PRINTING ) m_lastPrinting = value != 0;
        }
    }

    @Override
    public boolean canInteractWith( @Nonnull EntityPlayer player )
    {
        return m_printer.isUsableByPlayer( player );
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot( EntityPlayer player, int index )
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
            if( stack.getItem() == Items.DYE )
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
