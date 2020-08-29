/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.inventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.util.DefaultPropertyDelegate;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ContainerTurtle extends ScreenHandler implements IContainerComputer {
    private static final int PROPERTY_SLOT = 0;

    public final int m_playerInvStartY;
    public final int m_turtleInvStartX;

    private final Inventory inventory;
    private final PropertyDelegate properties;
    private final InputState input = new InputState(this);
    private IComputer computer;
    private int m_selectedSlot;

    public ContainerTurtle(int id, PlayerInventory playerInventory, ITurtleAccess turtle, IComputer computer) {
        this(id, playerInventory, turtle.getInventory(), new DefaultPropertyDelegate() {
            @Override
            public int get(int id) {
                return id == PROPERTY_SLOT ? turtle.getSelectedSlot() : 0;
            }

            @Override
            public int size() {
                return 1;
            }
        });
        this.computer = computer;
    }

    public ContainerTurtle(int id, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate properties) {
        this(id, playerInventory, inventory, properties, 134, 175);
    }

    private ContainerTurtle(int id, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate properties, int playerInvStartY,
                            int turtleInvStartX) {
        super(null, id);

        this.m_playerInvStartY = playerInvStartY;
        this.m_turtleInvStartX = turtleInvStartX;

        this.inventory = inventory;
        this.properties = properties;
        this.addProperties(properties);

        // Turtle inventory
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                this.addSlot(new Slot(inventory, x + y * 4, turtleInvStartX + 1 + x * 18, playerInvStartY + 1 + y * 18));
            }
        }

        // Player inventory
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, playerInvStartY + 1 + y * 18));
            }
        }

        // Player hotbar
        for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, playerInvStartY + 3 * 18 + 5));
        }
    }

    public int getSelectedSlot() {
        return this.properties.get(PROPERTY_SLOT);
    }

    @Nonnull
    @Override
    public ItemStack transferSlot(PlayerEntity player, int slotNum) {
        if (slotNum >= 0 && slotNum < 16) {
            return this.tryItemMerge(player, slotNum, 16, 52, true);
        } else if (slotNum >= 16) {
            return this.tryItemMerge(player, slotNum, 0, 16, false);
        }
        return ItemStack.EMPTY;
    }

    @Nonnull
    private ItemStack tryItemMerge(PlayerEntity player, int slotNum, int firstSlot, int lastSlot, boolean reverse) {
        Slot slot = this.slots.get(slotNum);
        ItemStack originalStack = ItemStack.EMPTY;
        if (slot != null && slot.hasStack()) {
            ItemStack clickedStack = slot.getStack();
            originalStack = clickedStack.copy();
            if (!this.insertItem(clickedStack, firstSlot, lastSlot, reverse)) {
                return ItemStack.EMPTY;
            }

            if (clickedStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (clickedStack.getCount() != originalStack.getCount()) {
                slot.onTakeItem(player, clickedStack);
            } else {
                return ItemStack.EMPTY;
            }
        }
        return originalStack;
    }

    @Override
    public void close(PlayerEntity player) {
        super.close(player);
        this.input.close();
    }

    @Override
    public boolean canUse(@Nonnull PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Nullable
    @Override
    public IComputer getComputer() {
        return this.computer;
    }

    @Nonnull
    @Override
    public InputState getInput() {
        return this.input;
    }
}
