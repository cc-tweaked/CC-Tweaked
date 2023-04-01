// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.impl;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.NonNullConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public final class Peripherals {
    private static final Logger LOG = LoggerFactory.getLogger(Peripherals.class);

    private static final Collection<IPeripheralProvider> providers = new LinkedHashSet<>();

    private Peripherals() {
    }

    public static synchronized void register(IPeripheralProvider provider) {
        Objects.requireNonNull(provider, "provider cannot be null");
        providers.add(provider);
    }

    @Nullable
    public static IPeripheral getPeripheral(Level world, BlockPos pos, Direction side, NonNullConsumer<Object> invalidate) {
        return world.isInWorldBounds(pos) && !world.isClientSide ? getPeripheralAt(world, pos, side, invalidate) : null;
    }

    @Nullable
    private static IPeripheral getPeripheralAt(Level world, BlockPos pos, Direction side, NonNullConsumer<? super Object> invalidate) {
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

        return GenericPeripheralProvider.getPeripheral(world, pos, side, invalidate);
    }

}
