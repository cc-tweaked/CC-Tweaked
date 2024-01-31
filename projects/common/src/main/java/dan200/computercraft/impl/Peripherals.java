// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.generic.ComponentLookup;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * The registry for peripheral providers.
 */
public final class Peripherals {
    private static final GenericPeripheralProvider genericProvider = new GenericPeripheralProvider();

    private Peripherals() {
    }

    public static void addGenericLookup(ComponentLookup lookup) {
        genericProvider.registerLookup(lookup);
    }

    public static @Nullable IPeripheral getGenericPeripheral(ServerLevel level, BlockPos pos, Direction side, @Nullable BlockEntity blockEntity) {
        return genericProvider.getPeripheral(level, pos, side, blockEntity);
    }
}
