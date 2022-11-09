/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.util.InvisibleSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * A computer menu which does not have any visible inventory.
 * <p>
 * This adds invisible versions of the player's hotbars slots, to ensure they're synced to the client when changed.
 */
public class ComputerMenuWithoutInventory extends AbstractComputerMenu {
    public ComputerMenuWithoutInventory(
        MenuType<? extends AbstractComputerMenu> type, int id, Inventory player, Predicate<Player> canUse,
        ServerComputer computer, ComputerFamily family
    ) {
        super(type, id, canUse, family, computer, null);
        addSlots(player);
    }

    public ComputerMenuWithoutInventory(MenuType<? extends AbstractComputerMenu> type, int id, Inventory player, ComputerContainerData menuData) {
        super(type, id, p -> true, menuData.family(), null, menuData);
        addSlots(player);
    }

    private void addSlots(Inventory player) {
        for (var i = 0; i < 9; i++) addSlot(new InvisibleSlot(player, i));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
