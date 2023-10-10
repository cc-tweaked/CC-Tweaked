// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.container;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * A slot which only accepts items matching a predicate.
 */
public class ValidatingSlot extends Slot {
    private final Predicate<ItemStack> predicate;

    public ValidatingSlot(Container inventoryIn, int index, int xPosition, int yPosition, Predicate<ItemStack> predicate) {
        super(inventoryIn, index, xPosition, yPosition);
        this.predicate = predicate;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return predicate.test(stack);
    }
}
