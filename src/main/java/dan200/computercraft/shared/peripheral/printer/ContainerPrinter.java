/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
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
    private TilePrinter m_printer;
    private boolean m_lastPrinting;

    public ContainerPrinter( IInventory playerInventory, TilePrinter printer )
    {
        m_printer = printer;
        m_lastPrinting = false;

        // Ink slot
        addSlotToContainer( new Slot( m_printer, 0, 13, 35 ) );

        // In-tray
        for( int i = 0; i < 6; i++ )
        {
            addSlotToContainer( new Slot( m_printer, i + 1, 61 + i * 18, 22 ) );
        }

        // Out-tray
        for( int i = 0; i < 6; i++ )
        {
            addSlotToContainer( new Slot( m_printer, i + 7, 61 + i * 18, 49 ) );
        }

        // Player inv
        for( int j = 0; j < 3; j++ )
        {
            for( int i1 = 0; i1 < 9; i1++ )
            {
                addSlotToContainer( new Slot( playerInventory, i1 + j * 9 + 9, 8 + i1 * 18, 84 + j * 18 ) );
            }
        }

        // Player hotbar
        for( int k = 0; k < 9; k++ )
        {
            addSlotToContainer( new Slot( playerInventory, k, 8 + k * 18, 142 ) );
        }
    }

    public boolean isPrinting()
    {
        return m_lastPrinting;
    }

    @Override
    public void addListener( IContainerListener crafting )
    {
        super.addListener( crafting );
        crafting.sendWindowProperty( this, 0, m_printer.isPrinting() ? 1 : 0 );
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        if( !m_printer.getWorld().isRemote )
        {
            boolean printing = m_printer.isPrinting();
            for( IContainerListener listener : listeners )
            {
                if( printing != m_lastPrinting )
                {
                    listener.sendWindowProperty( this, 0, printing ? 1 : 0 );
                }
            }
            m_lastPrinting = printing;
        }
    }

    @Override
    public void updateProgressBar( int i, int j )
    {
        if( m_printer.getWorld().isRemote )
        {
            m_lastPrinting = (j > 0);
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
                if( itemstack1.getItem() == Items.DYE )
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
