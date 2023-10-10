// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A basic {@link ICapabilityProvider} which provides a single capability, returning the same instance for every
 * direction.
 * <p>
 * This is designed for use with {@link AttachCapabilitiesEvent}, to attach individual capabilities to a specific
 * block entity.
 *
 * @param <T> The capability to provide.
 */
public final class CapabilityProvider<T> implements ICapabilityProvider {
    private final Capability<T> cap;
    private final Supplier<T> supplier;
    private final BooleanSupplier isRemoved;
    private @Nullable LazyOptional<T> instance;

    private CapabilityProvider(Capability<T> cap, Supplier<T> supplier, BooleanSupplier isRemoved) {
        this.cap = Objects.requireNonNull(cap, "Capability cannot be null");
        this.supplier = supplier;
        this.isRemoved = isRemoved;
    }

    public static <T> CapabilityProvider<T> attach(AttachCapabilitiesEvent<?> event, ResourceLocation id, Capability<T> cap, Supplier<T> instance) {
        BooleanSupplier isRemoved
            = event.getObject() instanceof BlockEntity be ? be::isRemoved
            : event.getObject() instanceof Entity entity ? entity::isRemoved
            : () -> true;
        var provider = new CapabilityProvider<>(cap, instance, isRemoved);
        event.addCapability(id, provider);
        event.addListener(provider::invalidate);
        return provider;
    }

    public void invalidate() {
        instance = CapabilityUtil.invalidate(instance);
    }

    @Override
    public <U> LazyOptional<U> getCapability(Capability<U> cap, @Nullable Direction side) {
        if (cap != this.cap || isRemoved.getAsBoolean()) return LazyOptional.empty();

        var instance = this.instance;
        if (instance == null) {
            var created = supplier.get();
            instance = this.instance = created == null ? LazyOptional.empty() : LazyOptional.of(() -> created);
        }
        return instance.cast();
    }
}
