// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot which is invisible and cannot be interacted with.
 * <p>
 * This is used to ensure inventory slots (normally the hotbar) are synced between client and server, when not actually
 * visible in the GUI.
 */
public class InvisibleSlot extends Slot {
    public InvisibleSlot(Container container, int slot) {
        super(container, slot, 0, 0);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
