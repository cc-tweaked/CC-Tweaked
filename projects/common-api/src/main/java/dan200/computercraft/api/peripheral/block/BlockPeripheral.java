// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral.block;

import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.ApiStatus;

/**
 * A {@linkplain IPeripheral peripheral} that is provided for a block in a world.
 * <p>
 * This provides a way to tie peripheral back to the block or block entity that supplied it.
 *
 * @since 1.122.0
 */
public interface BlockPeripheral extends IPeripheral {
    /**
     * Get the block this peripheral targets.
     * <p>
     * Implementations do not need to validate the block/block entity is still present.
     * <p>
     * Consumers should <strong>NOT</strong> call this method directly. Use
     * {@link PeripheralTargets#getBlockTarget(IPeripheral)} instead.
     *
     * @return The block/block entity this peripheral targets.
     */
    @Override
    @ApiStatus.OverrideOnly
    BlockPeripheralTarget getTarget();
}
