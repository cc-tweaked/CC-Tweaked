/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.core.Direction;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Provides a constant (but invalidate-able) capability for each side.
 *
 * @param <T> The type of the produced capability.
 */
public final class SidedCaps<T> {
    private final Function<Direction, T> factory;
    private final boolean allowNull;
    private T[] values;
    private LazyOptional<T>[] caps;

    private SidedCaps(Function<Direction, T> factory, boolean allowNull) {
        this.factory = factory;
        this.allowNull = allowNull;
    }

    public static <T> SidedCaps<T> ofNonNull(Function<Direction, T> factory) {
        return new SidedCaps<>(factory, false);
    }

    public static <T> SidedCaps<T> ofNullable(Function<Direction, T> factory) {
        return new SidedCaps<>(factory, true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public LazyOptional<T> get(@Nullable Direction direction) {
        if (direction == null && !allowNull) return LazyOptional.empty();
        var index = direction == null ? 6 : direction.ordinal();

        var caps = this.caps;
        if (caps == null) {
            caps = this.caps = new LazyOptional[allowNull ? 7 : 6];
            values = (T[]) new Object[caps.length];
        }

        var cap = caps[index];
        return cap == null ? caps[index] = LazyOptional.of(() -> {
            var values = this.values;
            var value = values[index];
            return value == null ? values[index] = factory.apply(direction) : value;
        }) : cap;
    }

    public void invalidate() {
        if (caps != null) CapabilityUtil.invalidate(caps);
    }
}
