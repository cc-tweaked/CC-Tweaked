/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.export;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class JsonDump {
    public Map<String, String> itemNames = new TreeMap<>();
    public Map<String, Recipe> recipes = new TreeMap<>();

    public static class Recipe {
        public final String[][] inputs = new String[9][];
        public String output;
        public int count;

        public Recipe(ItemStack output) {
            this.output = ForgeRegistries.ITEMS.getKey(output.getItem()).toString();
            count = output.getCount();
        }

        public void setInput(int pos, Ingredient ingredient, Set<Item> trackedItems) {
            if (ingredient.isEmpty()) return;

            var items = ingredient.getItems();

            // First try to simplify some tags to something easier.
            for (var stack : items) {
                var item = stack.getItem();
                if (!canonicalItem.contains(item)) continue;

                trackedItems.add(item);
                inputs[pos] = new String[]{ ForgeRegistries.ITEMS.getKey(item).toString() };
                return;
            }

            var itemIds = new String[items.length];
            for (var i = 0; i < items.length; i++) {
                var item = items[i].getItem();
                trackedItems.add(item);
                itemIds[i] = ForgeRegistries.ITEMS.getKey(item).toString();
            }
            Arrays.sort(itemIds);

            inputs[pos] = itemIds;
        }

        private static final Set<Item> canonicalItem = new HashSet<>(Arrays.asList(
            Items.GLASS_PANE, Items.STONE, Items.CHEST
        ));
    }
}
