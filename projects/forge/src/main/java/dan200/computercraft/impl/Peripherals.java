// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.impl;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.shared.peripheral.generic.ComponentLookup;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import dan200.computercraft.shared.platform.InvalidateCallback;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

/**
 * The registry for peripheral providers.
 * <p>
 * This lives in the {@code impl} package despite it not being part of the public API, in order to mirror Forge's class.
 */
public final class Peripherals {
    private static final Logger LOG = LoggerFactory.getLogger(Peripherals.class);

    private static final Collection<IPeripheralProvider> providers = new LinkedHashSet<>();
    private static final GenericPeripheralProvider<InvalidateCallback> genericProvider = new GenericPeripheralProvider<>();

    private Peripherals() {
    }

    public static synchronized void register(IPeripheralProvider provider) {
        Objects.requireNonNull(provider, "provider cannot be null");
        providers.add(provider);
    }

    public static void registerGenericLookup(ComponentLookup<InvalidateCallback> lookup) {
        genericProvider.registerLookup(lookup);
    }

    /**
     * A {@link ComponentLookup} for {@linkplain Capability capabilities}.
     * <p>
     * This is a record to ensure that adding the same capability multiple times only results in one lookup being
     * present in the resulting list.
     *
     * @param capability The capability to lookup
     * @param <T>        The type of the capability we look up.
     */
    private record CapabilityLookup<T>(Capability<T> capability) implements ComponentLookup<InvalidateCallback> {
        @Nullable
        @Override
        public T find(ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, Direction side, InvalidateCallback invalidate) {
            return CapabilityUtil.unwrap(CapabilityUtil.getCapability(blockEntity, this.capability(), side), invalidate);
        }
    }

    public static void registerGenericCapability(Capability<?> capability) {
        Objects.requireNonNull(capability, "Capability cannot be null");
        registerGenericLookup(new CapabilityLookup<>(capability));
    }

    @Nullable
    public static IPeripheral getPeripheral(ServerLevel world, BlockPos pos, Direction side, InvalidateCallback invalidate) {
        if (!world.isInWorldBounds(pos)) return null;

        var block = world.getBlockEntity(pos);
        if (block != null) {
            var peripheral = block.getCapability(CAPABILITY_PERIPHERAL, side);
            if (peripheral.isPresent()) return CapabilityUtil.unwrap(peripheral, invalidate);
        }

        // Try the handlers in order:
        for (var peripheralProvider : providers) {
            try {
                var peripheral = peripheralProvider.getPeripheral(world, pos, side);
                if (peripheral.isPresent()) return CapabilityUtil.unwrap(peripheral, invalidate);
            } catch (Exception e) {
                LOG.error("Peripheral provider " + peripheralProvider + " errored.", e);
            }
        }

        return genericProvider.getPeripheral(world, pos, side, block, invalidate);
    }
}
