// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.test.shared.WithMinecraft;
import dan200.computercraft.test.shared.platform.ItemContainerContract;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.world.Container;

@WithMinecraft
@SuppressWarnings("UnstableApiUsage")
public class FabricItemContainerTest implements ItemContainerContract {
    @Override
    public ItemContainer wrap(Container container) {
        return new FabricItemContainer(InventoryStorage.of(container, null));
    }

    @Override
    public ItemContainer wrapSlot(Container container, int slot) {
        return new FabricItemContainer(InventoryStorage.of(container, null).getSlot(slot));
    }

    @Override
    public ItemContainer wrapRotated(Container container, int offset) {
        return FabricItemContainer.rotated(InventoryStorage.of(container, null), offset);
    }
}
