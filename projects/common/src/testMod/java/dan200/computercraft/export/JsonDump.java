// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.export;

import dan200.computercraft.shared.util.RegistryHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class JsonDump {
    public Map<String, String> itemNames = new TreeMap<>();
    public Map<String, Recipe> recipes = new TreeMap<>();

    public static class Recipe {
        public final String[][] inputs = new String[9][];
        public String output;
        public int count;

        public Recipe(ItemStack output) {
            this.output = RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, output.getItem()).toString();
            count = output.getCount();
        }

        public void setInput(int pos, SlotDisplay ingredient, Set<Item> trackedItems) {
            if (ingredient instanceof SlotDisplay.Empty) return;

            var items = ingredient.resolveForStacks(new ContextMap.Builder().create(SlotDisplayContext.CONTEXT));

            // First try to simplify some tags to something easier.
            for (var stack : items) {
                var item = stack.getItem();
                if (!canonicalItem.contains(item)) continue;

                trackedItems.add(item);
                inputs[pos] = new String[]{ RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, item).toString() };
                return;
            }

            var itemIds = new String[items.size()];
            for (var i = 0; i < items.size(); i++) {
                var item = items.get(i).getItem();
                trackedItems.add(item);
                itemIds[i] = RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, item).toString();
            }
            Arrays.sort(itemIds);

            inputs[pos] = itemIds;
        }

        private static final Set<Item> canonicalItem = Set.of(
            Items.GLASS_PANE, Items.STONE, Items.CHEST
        );
    }
}
