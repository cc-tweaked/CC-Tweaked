/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.blocks.ComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class ComputerItemFactory {
    private ComputerItemFactory() {
    }

    public static ItemStack create(ComputerBlockEntity tile) {
        return create(tile.getComputerID(), tile.getLabel(), tile.getFamily());
    }

    public static ItemStack create(int id, @Nullable String label, ComputerFamily family) {
        return switch (family) {
            case NORMAL -> ModRegistry.Items.COMPUTER_NORMAL.get().create(id, label);
            case ADVANCED -> ModRegistry.Items.COMPUTER_ADVANCED.get().create(id, label);
            case COMMAND -> ModRegistry.Items.COMPUTER_COMMAND.get().create(id, label);
        };
    }
}
