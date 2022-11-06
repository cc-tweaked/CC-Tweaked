/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package net.minecraftforge.common.util;

public final class LazyOptional<T> {
    private LazyOptional() {
    }

    public static <T> LazyOptional<T> empty() {
        throw new UnsupportedOperationException();
    }
}
