/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package net.minecraftforge.registries;

@Deprecated
public class RegistryManager {
    public static final RegistryManager ACTIVE = new RegistryManager();

    private RegistryManager() {
    }

    public <T> IForgeRegistry<T> getRegistry(Object object) {
        throw new UnsupportedOperationException();
    }
}
