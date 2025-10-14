// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.test.shared.WithMinecraft;
import dan200.computercraft.test.shared.platform.ContainerTransferContract;
import net.minecraft.world.Container;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

@WithMinecraft
public class ForgeContainerTransferTest implements ContainerTransferContract {
    @Override
    public ContainerTransfer.Slotted wrap(Container container) {
        return new ForgeContainerTransfer(VanillaContainerWrapper.of(container));
    }
}
