// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.container.SingleContainerData;
import dan200.computercraft.shared.container.ValidatingSlot;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PrinterMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData properties;

    private PrinterMenu(int id, Inventory player, Container inventory, ContainerData properties) {
        super(ModRegistry.Menus.PRINTER.get(), id);
        this.properties = properties;
        this.inventory = inventory;

        addDataSlots(properties);

        // Ink slot
        addSlot(new ValidatingSlot(inventory, 0, 13, 35, PrinterBlockEntity::isInk));

        // In-tray
        for (var x = 0; x < 6; x++) {
            addSlot(new ValidatingSlot(inventory, x + 1, 61 + x * 18, 22, PrinterBlockEntity::isPaper));
        }

        // Out-tray
        for (var x = 0; x < 6; x++) addSlot(new ValidatingSlot(inventory, x + 7, 61 + x * 18, 49, o -> false));

        // Player inv
        for (var y = 0; y < 3; y++) {
            for (var x = 0; x < 9; x++) {
                addSlot(new Slot(player, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
            }
        }

        // Player hotbar
        for (var x = 0; x < 9; x++) {
            addSlot(new Slot(player, x, 8 + x * 18, 142));
        }
    }

    public PrinterMenu(int id, Inventory player) {
        this(id, player, new SimpleContainer(PrinterBlockEntity.SLOTS), new SimpleContainerData(1));
    }

    public PrinterMenu(int id, Inventory player, PrinterBlockEntity printer) {
        this(id, player, printer, (SingleContainerData) () -> printer.isPrinting() ? 1 : 0);
    }

    public boolean isPrinting() {
        return properties.get(0) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var result = stack.copy();
        if (index < 13) {
            // Transfer from printer to inventory
            if (!moveItemStackTo(stack, 13, 49, true)) return ItemStack.EMPTY;
        } else {
            // Transfer from inventory to printer
            if (PrinterBlockEntity.isInk(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                // Move to the paper slots
                if (!moveItemStackTo(stack, 1, 13, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;

        slot.onTake(player, stack);
        return result;
    }
}
