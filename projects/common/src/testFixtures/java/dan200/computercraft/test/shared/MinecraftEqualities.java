// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import dan200.computercraft.test.core.StructuralEquality;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.hamcrest.Description;

/**
 * {@link StructuralEquality} implementations for Minecraft types.
 */
public class MinecraftEqualities {
    public static final StructuralEquality<ItemStack> itemStack = new StructuralEquality<>() {
        @Override
        public boolean equals(ItemStack left, ItemStack right) {
            return ItemStack.isSameItemSameTags(left, right) && left.getCount() == right.getCount();
        }

        @Override
        public void describe(Description description, ItemStack object) {
            description.appendValue(object).appendValue(object.getTag());
        }
    };

    public static final StructuralEquality<Ingredient> ingredient = new StructuralEquality<>() {
        @Override
        public boolean equals(Ingredient left, Ingredient right) {
            return left.toJson().equals(right.toJson());
        }

        @Override
        public void describe(Description description, Ingredient object) {
            description.appendValue(object.toJson());
        }
    };
}
