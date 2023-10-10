// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.details;

import com.google.gson.JsonParseException;
import dan200.computercraft.shared.platform.RegistryWrappers;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Data providers for items.
 */
public class ItemDetails {
    public static void fillBasic(Map<? super String, Object> data, ItemStack stack) {
        data.put("name", DetailHelpers.getId(RegistryWrappers.ITEMS, stack.getItem()));
        data.put("count", stack.getCount());
        var hash = NBTUtil.getNBTHash(stack.getTag());
        if (hash != null) data.put("nbt", hash);
    }

    public static void fill(Map<? super String, Object> data, ItemStack stack) {
        data.put("displayName", stack.getHoverName().getString());
        data.put("maxCount", stack.getMaxStackSize());

        if (stack.isDamageableItem()) {
            data.put("damage", stack.getDamageValue());
            data.put("maxDamage", stack.getMaxDamage());
        }

        if (stack.getItem().isBarVisible(stack)) {
            data.put("durability", stack.getItem().getBarWidth(stack) / 13.0);
        }

        data.put("tags", DetailHelpers.getTags(stack.getTags()));

        // Include deprecated itemGroups field
        data.put("itemGroups", List.of());

        var tag = stack.getTag();
        if (tag != null && tag.contains("display", Tag.TAG_COMPOUND)) {
            var displayTag = tag.getCompound("display");
            if (displayTag.contains("Lore", Tag.TAG_LIST)) {
                var loreTag = displayTag.getList("Lore", Tag.TAG_STRING);
                data.put("lore", loreTag.stream()
                    .map(ItemDetails::parseTextComponent)
                    .filter(Objects::nonNull)
                    .map(Component::getString)
                    .toList());
            }
        }

        /*
         * Used to hide some data from ItemStack tooltip.
         * @see https://minecraft.wiki/w/Tutorials/Command_NBT_tags
         * @see ItemStack#getTooltip
         */
        var hideFlags = tag != null ? tag.getInt("HideFlags") : 0;

        var enchants = getAllEnchants(stack, hideFlags);
        if (!enchants.isEmpty()) data.put("enchantments", enchants);

        if (tag != null && tag.getBoolean("Unbreakable") && (hideFlags & 4) == 0) {
            data.put("unbreakable", true);
        }
    }

    @Nullable
    private static Component parseTextComponent(Tag x) {
        try {
            return Component.Serializer.fromJson(x.getAsString());
        } catch (JsonParseException e) {
            return null;
        }
    }

    /**
     * Retrieve all visible enchantments from given stack. Try to follow all tooltip rules : order and visibility.
     *
     * @param stack     Stack to analyse
     * @param hideFlags An int used as bit field to provide visibility rules.
     * @return A filled list that contain all visible enchantments.
     */
    private static List<Map<String, Object>> getAllEnchants(ItemStack stack, int hideFlags) {
        var enchants = new ArrayList<Map<String, Object>>(0);

        if (stack.getItem() instanceof EnchantedBookItem && (hideFlags & 32) == 0) {
            addEnchantments(EnchantedBookItem.getEnchantments(stack), enchants);
        }

        if (stack.isEnchanted() && (hideFlags & 1) == 0) {
            /*
             * Mimic the EnchantmentHelper.getEnchantments(ItemStack stack) behavior without special case for Enchanted book.
             * I'll do that to have the same data than ones displayed in tooltip.
             * @see EnchantmentHelper.getEnchantments(ItemStack stack)
             */
            addEnchantments(stack.getEnchantmentTags(), enchants);
        }

        return enchants;
    }

    /**
     * Converts a Mojang enchant map to a Lua list.
     *
     * @param rawEnchants The raw NBT list of enchantments
     * @param enchants    The enchantment map to add it to.
     * @see EnchantmentHelper
     */
    @SuppressWarnings("NonApiType")
    private static void addEnchantments(ListTag rawEnchants, ArrayList<Map<String, Object>> enchants) {
        if (rawEnchants.isEmpty()) return;

        enchants.ensureCapacity(enchants.size() + rawEnchants.size());

        for (var entry : EnchantmentHelper.deserializeEnchantments(rawEnchants).entrySet()) {
            var enchantment = entry.getKey();
            var level = entry.getValue();
            var enchant = new HashMap<String, Object>(3);
            enchant.put("name", DetailHelpers.getId(RegistryWrappers.ENCHANTMENTS, enchantment));
            enchant.put("level", level);
            enchant.put("displayName", enchantment.getFullname(level).getString());
            enchants.add(enchant);
        }
    }
}
