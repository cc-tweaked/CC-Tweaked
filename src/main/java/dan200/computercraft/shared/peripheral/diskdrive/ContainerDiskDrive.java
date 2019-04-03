/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.diskdrive;

import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerDiskDrive extends Container
{
    private final Inventory m_diskDrive;

    public ContainerDiskDrive( int id, PlayerInventory player )
    {
        this( id, player, new BasicInventory( TileDiskDrive.INVENTORY_SIZE ) );
    }

    public ContainerDiskDrive( int id, PlayerInventory playerInventory, Inventory diskDrive )
    {
        super( null, id );

        m_diskDrive = diskDrive;

        addSlot( new Slot( m_diskDrive, 0, 8 + 4 * 18, 35 ) );

        for( int y = 0; y < 3; y++ )
        {
            for( int x = 0; x < 9; x++ )
            {
                addSlot( new Slot( playerInventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18 ) );
            }
        }

        for( int x = 0; x < 9; x++ )
        {
            addSlot( new Slot( playerInventory, x, 8 + x * 18, 142 ) );
        }
    }

    @Override
    public boolean canUse( @Nonnull PlayerEntity player )
    {
        return m_diskDrive.canPlayerUseInv( player );
    }

    @Override
    public ItemStack transferSlot( PlayerEntity player, int slotIndex )
    {
        Slot slot = slotList.get( slotIndex );
        if( slot == null || !slot.hasStack() ) return ItemStack.EMPTY;

        ItemStack existing = slot.getStack();
        ItemStack result = existing.copy();
        if( slotIndex == 0 )
        {
            // Insert into player inventory
            if( !insertItem( existing, 1, 37, true ) ) return ItemStack.EMPTY;
        }
        else
        {
            // Insert into drive inventory
            if( !insertItem( existing, 0, 1, false ) ) return ItemStack.EMPTY;
        }

        if( existing.isEmpty() )
        {
            slot.setStack( ItemStack.EMPTY );
        }
        else
        {
            slot.markDirty();
        }

        if( existing.getAmount() == result.getAmount() ) return ItemStack.EMPTY;

        slot.onTakeItem( player, existing );
        return result;
    }
}
