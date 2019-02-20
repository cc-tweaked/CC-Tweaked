/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerPrinter extends Container
{
    private static final int PROPERTY_PRINTING = 0;

    private TilePrinter m_printer;
    private boolean m_lastPrinting;

    public ContainerPrinter( IInventory playerInventory, TilePrinter printer )
    {
        m_printer = printer;
        m_lastPrinting = false;

        // Ink slot
        addSlot( new Slot( printer, 0, 13, 35 ) );

        // In-tray
        for( int x = 0; x < 6; x++ ) addSlot( new Slot( printer, x + 1, 61 + x * 18, 22 ) );

        // Out-tray
        for( int x = 0; x < 6; x++ ) addSlot( new Slot( printer, x + 7, 61 + x * 18, 49 ) );

        // Player inv
        for( int y = 0; y < 3; y++ )
        {
            for( int x = 0; x < 9; x++ )
            {
                addSlot( new Slot( playerInventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18 ) );
            }
        }

        // Player hotbar
        for( int x = 0; x < 9; x++ )
        {
            addSlot( new Slot( playerInventory, x, 8 + x * 18, 142 ) );
        }
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
            // Push the printing state to the client if needed.
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
        super.updateProgressBar( property, value );
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
    public ItemStack transferStackInSlot( EntityPlayer par1EntityPlayer, int i )
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get( i );
        if( slot != null && slot.getHasStack() )
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if( i < 13 )
            {
                // Transfer from printer to inventory
                if( !mergeItemStack( itemstack1, 13, 49, true ) )
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // Transfer from inventory to printer
                if( itemstack1.getItem() instanceof ItemDye )
                {
                    if( !mergeItemStack( itemstack1, 0, 1, false ) )
                    {
                        return ItemStack.EMPTY;
                    }
                }
                else //if is paper
                {
                    if( !mergeItemStack( itemstack1, 1, 13, false ) )
                    {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if( itemstack1.isEmpty() )
            {
                slot.putStack( ItemStack.EMPTY );
            }
            else
            {
                slot.onSlotChanged();
            }

            if( itemstack1.getCount() != itemstack.getCount() )
            {
                slot.onTake( par1EntityPlayer, itemstack1 );
            }
            else
            {
                return ItemStack.EMPTY;
            }
        }
        return itemstack;
    }
}
