// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.NonNullConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;

public class GenericPeripheralProvider {
    private static final Logger LOG = LoggerFactory.getLogger(GenericPeripheralProvider.class);

    private static final ArrayList<Capability<?>> capabilities = new ArrayList<>();

    public static synchronized void addCapability(Capability<?> capability) {
        Objects.requireNonNull(capability, "Capability cannot be null");
        if (!capabilities.contains(capability)) capabilities.add(capability);
    }

    @Nullable
    public static IPeripheral getPeripheral(Level level, BlockPos pos, Direction side, NonNullConsumer<Object> invalidate) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;

        var server = level.getServer();
        if (server == null) {
            LOG.warn("Fetching peripherals on a non-server level {}.", level, new IllegalStateException("Fetching peripherals on a non-server level."));
            return null;
        }

        var builder = new GenericPeripheralBuilder(server);
        builder.addMethods(blockEntity);

        for (var capability : capabilities) {
            var wrapper = CapabilityUtil.getCapability(blockEntity, capability, side);
            wrapper.ifPresent(contents -> {
                if (builder.addMethods(contents)) CapabilityUtil.addListener(wrapper, invalidate);
            });
        }

        return builder.toPeripheral(blockEntity, side);
    }
}
