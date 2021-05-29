/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.shared.Registry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerDiskDrive extends Container
{
    private final IInventory inventory;

    public ContainerDiskDrive( int id, PlayerInventory player, IInventory inventory )
    {
        super( Registry.ModContainers.DISK_DRIVE.get(), id );

        this.inventory = inventory;

        addSlot( new Slot( this.inventory, 0, 8 + 4 * 18, 35 ) );

        for( int y = 0; y < 3; y++ )
        {
            for( int x = 0; x < 9; x++ )
            {
                addSlot( new Slot( player, x + y * 9 + 9, 8 + x * 18, 84 + y * 18 ) );
            }
        }

        for( int x = 0; x < 9; x++ )
        {
            addSlot( new Slot( player, x, 8 + x * 18, 142 ) );
        }
    }

    public ContainerDiskDrive( int id, PlayerInventory player )
    {
        this( id, player, new Inventory( 1 ) );
    }

    @Override
    public boolean stillValid( @Nonnull PlayerEntity player )
    {
        return inventory.stillValid( player );
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack( @Nonnull PlayerEntity player, int slotIndex )
    {
        Slot slot = slots.get( slotIndex );
        if( slot == null || !slot.hasItem() ) return ItemStack.EMPTY;

        ItemStack existing = slot.getItem().copy();
        ItemStack result = existing.copy();
        if( slotIndex == 0 )
        {
            // Insert into player inventory
            if( !moveItemStackTo( existing, 1, 37, true ) ) return ItemStack.EMPTY;
        }
        else
        {
            // Insert into drive inventory
            if( !moveItemStackTo( existing, 0, 1, false ) ) return ItemStack.EMPTY;
        }

        if( existing.isEmpty() )
        {
            slot.set( ItemStack.EMPTY );
        }
        else
        {
            slot.setChanged();
        }

        if( existing.getCount() == result.getCount() ) return ItemStack.EMPTY;

        slot.onTake( player, existing );
        return result;
    }
}
