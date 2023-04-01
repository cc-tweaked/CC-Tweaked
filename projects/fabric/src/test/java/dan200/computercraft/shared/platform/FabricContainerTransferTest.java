// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.test.shared.WithMinecraft;
import dan200.computercraft.test.shared.platform.ContainerTransferContract;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.world.Container;

@WithMinecraft
public class FabricContainerTransferTest implements ContainerTransferContract {
    @Override
    @SuppressWarnings("UnstableApiUsage")
    public ContainerTransfer.Slotted wrap(Container container) {
        return FabricContainerTransfer.of(InventoryStorage.of(container, null));
    }
}
