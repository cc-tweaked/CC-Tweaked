// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import dan200.computercraft.test.core.StructuralEquality;
import net.minecraft.world.item.ItemStack;
import org.hamcrest.Description;

/**
 * {@link StructuralEquality} implementations for Minecraft types.
 */
public class MinecraftEqualities {
    public static final StructuralEquality<ItemStack> itemStack = new StructuralEquality<>() {
        @Override
        public boolean equals(ItemStack left, ItemStack right) {
            return ItemStack.isSameItemSameComponents(left, right) && left.getCount() == right.getCount();
        }

        @Override
        public void describe(Description description, ItemStack object) {
            description.appendValue(object).appendValue(object.getComponentsPatch());
        }
    };
}
