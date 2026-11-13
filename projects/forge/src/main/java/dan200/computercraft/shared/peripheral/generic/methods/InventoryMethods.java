// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.peripheral.ForgePeripheralTargets;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.ForgeItemContainer;
import dan200.computercraft.shared.platform.ItemContainer;
import net.neoforged.neoforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;

/**
 * Inventory methods for Forge's {@link IItemHandler}.
 */
public final class InventoryMethods extends AbstractInventoryMethods<IItemHandler> {
    @Override
    protected ItemContainer getContainer(IItemHandler inventory) {
        return new ForgeItemContainer(inventory);
    }

    @Override
    protected @Nullable ItemContainer getContainer(IPeripheral peripheral) {
        var handler = ForgePeripheralTargets.getItemHandler(peripheral);
        return handler == null ? null : new ForgeItemContainer(handler);
    }
}
