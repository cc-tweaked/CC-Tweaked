// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.test.shared.WithMinecraft;
import dan200.computercraft.test.shared.platform.ItemContainerContract;
import net.minecraft.world.Container;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

@WithMinecraft
public class ForgeItemContainerTest implements ItemContainerContract {
    @Override
    public ItemContainer wrap(Container container) {
        return new ForgeItemContainer(new InvWrapper(container));
    }

    @Override
    public ItemContainer wrapSlot(Container container, int slot) {
        return wrap(container).singleSlot(slot);
    }

    @Override
    public ItemContainer wrapRotated(Container container, int offset) {
        return ForgeItemContainer.rotated(new InvWrapper(container), offset);
    }
}
