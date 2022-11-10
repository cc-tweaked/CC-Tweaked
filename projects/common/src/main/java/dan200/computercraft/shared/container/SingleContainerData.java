/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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
