// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.test.shared.peripheral.generic.methods.InventoryMethodsContract;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

public class InventoryMethodsTest implements InventoryMethodsContract<InventoryMethods.StorageWrapper> {
    @Override
    public AbstractInventoryMethods<InventoryMethods.StorageWrapper> create() {
        return new InventoryMethods(RegistryAccess.EMPTY);
    }

    @Override
    public InventoryMethods.StorageWrapper wrap(Container container) {
        return new InventoryMethods.StorageWrapper(VanillaContainerWrapper.of(container));
    }
}
