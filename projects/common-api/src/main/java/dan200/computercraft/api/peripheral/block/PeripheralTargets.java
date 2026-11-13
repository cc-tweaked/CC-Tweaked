// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral.block;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.impl.ComputerCraftAPIService;
import org.jspecify.annotations.Nullable;

/**
 * Helpers for extracting objects from the {@linkplain IPeripheral#getTarget() target} of a peripheral.
 * <p>
 * There are also Forge ({@code dan200.computercraft.api.peripheral.ForgePeripheralTargets}) and Fabric
 * ({@code dan200.computercraft.api.peripheral.FabricPeripheralTargets}) variants of this class.
 *
 * @since 1.122.0
 */
public final class PeripheralTargets {
    private PeripheralTargets() {
    }

    /**
     * Get the block this peripheral targets.
     * <p>
     * The returned target is guaranteed to still be loaded and present in the world.
     *
     * @param peripheral The peripheral to extract the target from.
     * @return The peripheral's target, if known.
     * @see IPeripheral#getTarget()
     * @see BlockPeripheral#getTarget()
     */
    public static @Nullable BlockPeripheralTarget getBlockTarget(IPeripheral peripheral) {
        var target = ComputerCraftAPIService.get().getPeripheralTarget(peripheral);
        return target != null && isTargetValid(target) ? target : null;
    }

    private static boolean isTargetValid(BlockPeripheralTarget source) {
        if (!source.getLevel().isLoaded(source.getPos())) return false;

        var blockEntity = source.getBlockEntity();
        if (blockEntity != null && blockEntity.isRemoved()) return false;

        return true;
    }
}
