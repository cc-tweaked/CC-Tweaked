// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;

/**
 * An abstraction over containers of items, such as {@link Container} and mod-loader specific inventory APIs.
 */
public interface ItemContainer {
    int NO_ITEMS = -1;
    int NO_SPACE = -2;

    int size();

    Iterator<ItemStack> contents();

    ItemStack getItem(int slot);

    int getCapacity(int slot);

    @Nullable ItemContainer singleSlot(int slot);

    /**
     * Push an item from this container to another.
     *
     * @param destination The container to push to.
     * @param maxAmount   The maximum number of items to move.
     * @return The number of items which were transferred, or one of {@link #NO_ITEMS} or {@link #NO_SPACE}. This will
     * <em>NEVER</em> return 0.
     */
    int moveTo(ItemContainer destination, int maxAmount);

    /**
     * Push an item from this container to another.
     *
     * @param fromSlot    The slot to move items from.
     * @param destination The container to push to.
     * @param maxAmount   The maximum number of items to move.
     * @return The number of items which were transferred, or one of {@link #NO_ITEMS} or {@link #NO_SPACE}. This will
     * <em>NEVER</em> return 0.
     */
    int moveTo(int fromSlot, ItemContainer destination, int maxAmount);
}
