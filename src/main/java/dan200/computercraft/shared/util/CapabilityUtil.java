/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CapabilityUtil {
    private CapabilityUtil() {
    }

    @Nullable
    public static <T> LazyOptional<T> invalidate(@Nullable LazyOptional<T> cap) {
        if (cap != null) cap.invalidate();
        return null;
    }

    public static <T> void invalidate(@Nullable LazyOptional<T>[] caps) {
        if (caps == null) return;

        for (var i = 0; i < caps.length; i++) {
            var cap = caps[i];
            if (cap != null) cap.invalidate();
            caps[i] = null;
        }
    }

    public static <T> void addListener(LazyOptional<T> p, NonNullConsumer<? super LazyOptional<T>> invalidate) {
        // We can make this safe with invalidate::accept, but then we're allocating it's just kind of absurd.
        @SuppressWarnings("unchecked")
        var safeInvalidate = (NonNullConsumer<LazyOptional<T>>) invalidate;
        p.addListener(safeInvalidate);
    }

    @Nullable
    public static <T> T unwrap(LazyOptional<T> p, NonNullConsumer<? super LazyOptional<T>> invalidate) {
        if (!p.isPresent()) return null;

        addListener(p, invalidate);
        return p.orElseThrow(NullPointerException::new);
    }

    @Nullable
    public static <T> T unwrapUnsafe(LazyOptional<T> p) {
        return !p.isPresent() ? null : p.orElseThrow(NullPointerException::new);
    }

    /**
     * Find a capability, preferring the internal/null side but falling back to a given side if a mod doesn't support
     * the internal one.
     *
     * @param provider   The capability provider to get the capability from.
     * @param capability The capability to get.
     * @param side       The side we'll fall back to.
     * @param <T>        The type of the underlying capability.
     * @return The extracted capability, if present.
     */
    @Nonnull
    public static <T> LazyOptional<T> getCapability(ICapabilityProvider provider, Capability<T> capability, Direction side) {
        var cap = provider.getCapability(capability);
        return cap.isPresent() ? cap : provider.getCapability(capability, side);
    }
}
