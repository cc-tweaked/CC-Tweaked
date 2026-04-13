// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.details;

import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * Data providers for items.
 */
public class ItemDetails {
    public static void fillBasic(Map<? super String, Object> data, HolderLookup.Provider registries, ItemStack stack) {
        data.put("name", DetailHelpers.getId(BuiltInRegistries.ITEM, stack.getItem()));
        data.put("count", stack.getCount());

        var hash = getComponentHash(registries, stack.getComponentsPatch());
        if (hash != null) data.put("nbt", hash);
    }

    private static @Nullable String getComponentHash(HolderLookup.Provider registries, DataComponentPatch components) {
        if (components.isEmpty()) return null;

        var nbt = DataComponentPatch.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), components)
            .result().orElse(null);
        return NBTUtil.getNBTHash(nbt);
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

        data.put("tags", DetailHelpers.getTags(stack));
        data.put("itemGroups", getItemGroups(stack));

        var lore = stack.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            data.put("lore", lore.lines().stream().map(Component::getString).toList());
        }

        var enchants = getAllEnchants(stack);
        if (!enchants.isEmpty()) data.put("enchantments", enchants);

        var effects = getAllEffects(stack);
        if (!effects.isEmpty()) data.put("potionEffects", effects);

        var unbreakable = stack.get(DataComponents.UNBREAKABLE);
        if (unbreakable != null) data.put("unbreakable", true);

        DetailHelpers.fillMapColour(data, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Block.byItem(stack.getItem()).defaultBlockState());
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

    /**
     * Retrieve all potions from given stack.
     *
     * @param stack Stack to analyse.
     * @return A filled list that contain all visible potions.
     */
    private static List<Map<String, Object>> getAllEffects(ItemStack stack) {
        var effects = stack.get(DataComponents.POTION_CONTENTS);
        if (effects == null) return List.of();

        return StreamSupport.stream(effects.getAllEffects().spliterator(), false).map(p -> {
            Map<String, Object> potion = new HashMap<>(4);
            potion.put("name", p.getEffect().getRegisteredName());

            var displayName = Component.translatable(p.getDescriptionId());
            if (p.getAmplifier() > 0) {
                displayName = Component.translatable(
                    "potion.withAmplifier", displayName, Component.translatable("potion.potency." + p.getAmplifier())
                );
            }
            potion.put("displayName", displayName.getString());

            // Expose the roman numerals (e.g. Instant Health II), rather than the raw amplifier value.
            if (p.getAmplifier() > 0) potion.put("potency", p.getAmplifier() + 1);

            if (p.isInfiniteDuration()) {
                potion.put("duration", Double.POSITIVE_INFINITY);
            } else if (p.getDuration() > 1) {
                potion.put("duration", p.getDuration() / 20.0 * getPotionDurationMultiplier(stack));
            }
            return potion;
        }).toList();
    }

    /**
     * Get the potion duration multiplier for an item, to handle items which have a shorter duration than a normal
     * potion.
     *
     * @param stack The current stack.
     * @return The duration multiplier.
     */
    private static double getPotionDurationMultiplier(ItemStack stack) {
        if (stack.getItem() instanceof LingeringPotionItem) return 0.25;
        if (stack.getItem() instanceof TippedArrowItem) return 0.125;
        return 1.0;
    }
}
