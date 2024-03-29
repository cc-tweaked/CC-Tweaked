// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.platform.InvalidateCallback;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

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

    @Nullable
    public static <T> T unwrap(LazyOptional<T> p, InvalidateCallback invalidate) {
        if (!p.isPresent()) return null;

        p.addListener(invalidate.castConsumer());
        return p.orElseThrow(NullPointerException::new);
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
    public static <T> LazyOptional<T> getCapability(ICapabilityProvider provider, Capability<T> capability, @Nullable Direction side) {
        var cap = provider.getCapability(capability);
        return !cap.isPresent() && side != null ? provider.getCapability(capability, side) : cap;
    }
}
