// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.container;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A container backed by a simple list.
 *
 * @param items The items backing this list.
 */
public record ListContainer(List<ItemStack> items) implements BasicContainer {
    @Override
    public List<ItemStack> getItems() {
        return items;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
