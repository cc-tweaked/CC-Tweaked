// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.shared.ModRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DiskDriveMenu extends AbstractContainerMenu {
    private final Container inventory;

    public DiskDriveMenu(int id, Inventory player, Container inventory) {
        super(ModRegistry.Menus.DISK_DRIVE.get(), id);

        this.inventory = inventory;

        addSlot(new Slot(this.inventory, 0, 8 + 4 * 18, 35));

        for (var y = 0; y < 3; y++) {
            for (var x = 0; x < 9; x++) {
                addSlot(new Slot(player, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
            }
        }

        for (var x = 0; x < 9; x++) {
            addSlot(new Slot(player, x, 8 + x * 18, 142));
        }
    }

    public DiskDriveMenu(int id, Inventory player) {
        this(id, player, new SimpleContainer(1));
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        var slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        var existing = slot.getItem().copy();
        var result = existing.copy();
        if (slotIndex == 0) {
            // Insert into player inventory
            if (!moveItemStackTo(existing, 1, 37, true)) return ItemStack.EMPTY;
        } else {
            // Insert into drive inventory
            if (!moveItemStackTo(existing, 0, 1, false)) return ItemStack.EMPTY;
        }

        if (existing.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (existing.getCount() == result.getCount()) return ItemStack.EMPTY;

        slot.onTake(player, existing);
        return result;
    }
}
