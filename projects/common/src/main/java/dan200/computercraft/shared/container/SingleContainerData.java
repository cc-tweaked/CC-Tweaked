// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.container;

import net.minecraft.world.inventory.ContainerData;

/**
 * A basic {@link ContainerData} implementation which provides a single value.
 */
@FunctionalInterface
public interface SingleContainerData extends ContainerData {
    int get();

    @Override
    default int get(int property) {
        return property == 0 ? get() : 0;
    }

    @Override
    default void set(int property, int value) {
    }

    @Override
    default int getCount() {
        return 1;
    }
}
