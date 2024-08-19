// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.details;

import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data providers for items.
 */
public class ItemDetails {
    public static void fillBasic(Map<? super String, Object> data, ItemStack stack) {
        data.put("name", DetailHelpers.getId(BuiltInRegistries.ITEM, stack.getItem()));
        data.put("count", stack.getCount());

        var components = stack.getComponentsPatch();
        var hash = components.isEmpty() ? null : NBTUtil.getNBTHash(DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, components).result().orElse(null));
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
        data.put("itemGroups", getItemGroups(stack));

        var lore = stack.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            data.put("lore", lore.lines().stream().map(Component::getString).toList());
        }

        var enchants = getAllEnchants(stack);
        if (!enchants.isEmpty()) data.put("enchantments", enchants);

        var unbreakable = stack.get(DataComponents.UNBREAKABLE);
        if (unbreakable != null && unbreakable.showInTooltip()) data.put("unbreakable", true);
    }

    /**
     * Retrieve all item groups an item stack pertains to.
     *
     * @param stack Stack to analyse
     * @return A filled list that contains pairs of item group IDs and their display names.
     */
    private static List<Map<String, Object>> getItemGroups(ItemStack stack) {
        return CreativeModeTabs.allTabs().stream()
            .filter(x -> x.shouldDisplay() && x.getType() == CreativeModeTab.Type.CATEGORY && x.contains(stack))
            .map(group -> {
                Map<String, Object> groupData = new HashMap<>(2);

                var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(group);
                if (id != null) groupData.put("id", id.toString());

                groupData.put("displayName", group.getDisplayName().getString());
                return groupData;
            })
            .toList();
    }

    /**
     * Retrieve all visible enchantments from given stack. Try to follow all tooltip rules : order and visibility.
     *
     * @param stack Stack to analyse
     * @return A filled list that contain all visible enchantments.
     */
    private static List<Map<String, Object>> getAllEnchants(ItemStack stack) {
        var enchants = new ArrayList<Map<String, Object>>(0);
        addEnchantments(stack.get(DataComponents.STORED_ENCHANTMENTS), enchants);
        addEnchantments(stack.get(DataComponents.ENCHANTMENTS), enchants);
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
    private static void addEnchantments(@Nullable ItemEnchantments rawEnchants, ArrayList<Map<String, Object>> enchants) {
        if (rawEnchants == null || rawEnchants.isEmpty()) return;

        enchants.ensureCapacity(enchants.size() + rawEnchants.size());

        for (var entry : rawEnchants.entrySet()) {
            var enchantment = entry.getKey();
            var level = entry.getIntValue();
            var enchant = new HashMap<String, Object>(3);
            enchant.put("name", enchantment.getRegisteredName());
            enchant.put("level", level);
            enchant.put("displayName", Enchantment.getFullname(enchantment, level).getString());
            enchants.add(enchant);
        }
    }
}
