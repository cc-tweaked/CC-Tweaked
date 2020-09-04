/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import javax.annotation.Nonnull;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;

import net.minecraft.item.ItemStack;

public final class ComputerItemFactory {
    private ComputerItemFactory() {}

    @Nonnull
    public static ItemStack create(TileComputer tile) {
        return create(tile.getComputerID(), tile.getLabel(), tile.getFamily());
    }

    @Nonnull
    public static ItemStack create(int id, String label, ComputerFamily family) {
        switch (family) {
        case NORMAL:
            return ComputerCraftRegistry.ModItems.COMPUTER_NORMAL.create(id, label);
        case ADVANCED:
            return ComputerCraftRegistry.ModItems.COMPUTER_ADVANCED.create(id, label);
        case COMMAND:
            return ComputerCraftRegistry.ModItems.COMPUTER_COMMAND.create(id, label);
        default:
            return ItemStack.EMPTY;
        }
    }
}
