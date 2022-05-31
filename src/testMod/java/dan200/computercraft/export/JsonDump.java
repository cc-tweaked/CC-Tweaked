/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.export;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;

import java.util.*;

public class JsonDump
{
    public Map<String, String> itemNames = new TreeMap<>();
    public Map<String, Recipe> recipes = new TreeMap<>();

    public static class Recipe
    {
        public final String[][] inputs = new String[9][];
        public String output;
        public int count;

        public Recipe( ItemStack output )
        {
            this.output = output.getItem().getRegistryName().toString();
            count = output.getCount();
        }

        public void setInput( int pos, Ingredient ingredient, Set<Item> trackedItems )
        {
            if( ingredient.isEmpty() ) return;

            ItemStack[] items = ingredient.getItems();

            // First try to simplify some tags to something easier.
            for( ItemStack stack : items )
            {
                Item item = stack.getItem();
                if( !canonicalItem.contains( item ) ) continue;

                trackedItems.add( item );
                inputs[pos] = new String[] { item.getRegistryName().toString() };
                return;
            }

            String[] itemIds = new String[items.length];
            for( int i = 0; i < items.length; i++ )
            {
                Item item = items[i].getItem();
                trackedItems.add( item );
                itemIds[i] = item.getRegistryName().toString();
            }
            Arrays.sort( itemIds );

            inputs[pos] = itemIds;
        }

        private static final Set<Item> canonicalItem = new HashSet<>( Arrays.asList(
            Items.GLASS_PANE, Items.STONE, Items.CHEST
        ) );
    }
}
