// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Loader-specific recipe ingredients. These may either be tags or items, depending on which mod loader we're using.
 *
 * @param redstone    All {@link Items#REDSTONE} items.
 * @param string      All {@link Items#STRING} items.
 * @param leather     All {@link Items#LEATHER} items.
 * @param glassPane   All {@link Items#GLASS_PANE} items.
 * @param goldIngot   All {@link Items#GOLD_INGOT} items.
 * @param goldBlock   All {@link Items#GOLD_BLOCK} items.
 * @param ironIngot   All {@link Items#IRON_INGOT} items.
 * @param dye         All dye items.
 * @param enderPearl  All {@link Items#ENDER_PEARL} items.
 * @param woodenChest All wooden chests (both normal and trapped chests).
 */
public record RecipeIngredients(
    TagKey<Item> redstone,
    TagKey<Item> string,
    TagKey<Item> leather,
    TagKey<Item> glassPane,
    TagKey<Item> goldIngot,
    TagKey<Item> goldBlock,
    TagKey<Item> ironIngot,
    TagKey<Item> dye,
    TagKey<Item> enderPearl,
    TagKey<Item> woodenChest
) {
}
