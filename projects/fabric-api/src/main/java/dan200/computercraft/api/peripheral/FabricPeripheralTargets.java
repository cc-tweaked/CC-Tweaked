// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.peripheral.block.BlockPeripheralTarget;
import dan200.computercraft.api.peripheral.block.PeripheralTargets;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Fabric-specific helpers for extracting objects from the {@linkplain IPeripheral#getTarget() target} from a peripheral.
 *
 * @see PeripheralTargets
 * @since 1.122.0
 */
public final class FabricPeripheralTargets {
    private FabricPeripheralTargets() {
    }

    /**
     * Extract a sided {@link BlockApiLookup} (such as {@link ItemStorage#SIDED}) from a peripheral.
     * <p>
     * This function will first try to find the API on the internal/{@code null} side of the block, falling back to
     * the {@linkplain BlockPeripheralTarget#getSide() side the peripheral is attached on} if known.
     *
     * @param peripheral The peripheral to extract the object from.
     * @param lookup     The lookup to use.
     * @param <T>        The type of API to lookup.
     * @return The found API, if known.
     */
    @SuppressWarnings("NullAway") // FIXME(26.3): Can be removed when FAPI fixes their annotation
    public static <T> @Nullable T getSidedLookup(IPeripheral peripheral, BlockApiLookup<T, @Nullable Direction> lookup) {
        var blockTarget = PeripheralTargets.getBlockTarget(peripheral);
        if (blockTarget != null) {
            var blockState = blockTarget.getBlockState();

            var result = lookup.find(blockTarget.getLevel(), blockTarget.getPos(), blockState, blockTarget.getBlockEntity(), null);
            if (result != null) return result;

            if (blockTarget.getSide() != null) {
                var sidedResult = lookup.find(blockTarget.getLevel(), blockTarget.getPos(), blockState, blockTarget.getBlockEntity(), blockTarget.getSide());
                if (sidedResult != null) return sidedResult;
            }
        }

        return null;
    }
}
