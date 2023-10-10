// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.world.Container;

/**
 * A quasi-{@link Container}, which just supports transferring items.
 */
public interface ContainerTransfer {
    int NO_ITEMS = -1;
    int NO_SPACE = -2;

    /**
     * Push an item from this container to another.
     *
     * @param destination The container to push to.
     * @param maxAmount   The maximum number of items to move.
     * @return The number of items which were transferred, or one of {@link #NO_ITEMS} or {@link #NO_SPACE}. This will
     * <em>NEVER</em> return 0.
     */
    int moveTo(ContainerTransfer destination, int maxAmount);

    /**
     * A {@link ContainerTransfer} which also has slots.
     */
    interface Slotted extends ContainerTransfer {
        /**
         * Create a new {@link ContainerTransfer} which rotates the inventory, so that inserts start at {@code offset}
         * instead.
         *
         * @param offset The slot offset
         * @return The new container transfer.
         */
        ContainerTransfer rotate(int offset);

        /**
         * Create a new {@link ContainerTransfer} which can view a single slot of this container.
         *
         * @param slot The slot we can view.
         * @return The new container transfer.
         */
        ContainerTransfer singleSlot(int slot);
    }
}
