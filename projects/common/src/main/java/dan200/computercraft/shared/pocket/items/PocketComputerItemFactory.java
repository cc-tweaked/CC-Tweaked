/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.items;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class PocketComputerItemFactory {
    private PocketComputerItemFactory() {
    }

    public static ItemStack create(int id, @Nullable String label, int colour, ComputerFamily family, @Nullable IPocketUpgrade upgrade) {
        return switch (family) {
            case NORMAL -> ModRegistry.Items.POCKET_COMPUTER_NORMAL.get().create(id, label, colour, upgrade);
            case ADVANCED -> ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get().create(id, label, colour, upgrade);
            default -> ItemStack.EMPTY;
        };
    }
}
