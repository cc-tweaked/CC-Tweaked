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
 * <p>
 * This lives in the {@code impl} package despite it not being part of the public API, in order to mirror Forge's class.
 */
public final class Peripherals {
    private static final GenericPeripheralProvider<Runnable> genericProvider = new GenericPeripheralProvider<>();

    private Peripherals() {
    }

    public static void addGenericLookup(ComponentLookup<? super Runnable> lookup) {
        genericProvider.registerLookup(lookup);
    }

    public static @Nullable IPeripheral getGenericPeripheral(ServerLevel level, BlockPos pos, Direction side, @Nullable BlockEntity blockEntity, Runnable invalidate) {
        return genericProvider.getPeripheral(level, pos, side, blockEntity, invalidate);
    }
}
