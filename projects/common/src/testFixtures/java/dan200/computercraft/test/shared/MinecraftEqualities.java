// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import dan200.computercraft.test.core.StructuralEquality;
import net.minecraft.Util;
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
        private static JsonElement toJson(Ingredient ingredient) {
            return Util.getOrThrow(Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient), JsonParseException::new);
        }

        @Override
        public boolean equals(Ingredient left, Ingredient right) {
            return toJson(left).equals(toJson(right));
        }

        @Override
        public void describe(Description description, Ingredient object) {
            description.appendValue(toJson(object));
        }
    };
}
