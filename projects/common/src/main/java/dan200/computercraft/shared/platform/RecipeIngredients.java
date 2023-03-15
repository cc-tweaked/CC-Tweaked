// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Loader-specific recipe ingredients. These may either be tags or items, depending on which mod loader we're using.
 *
 * @param redstone    All {@link Items#REDSTONE} items.
 * @param string      All {@link Items#STRING} items.
 * @param leather     All {@link Items#LEATHER} items.
 * @param stone       All {@link Items#STONE} items.
 * @param glassPane   All {@link Items#GLASS_PANE} items.
 * @param goldIngot   All {@link Items#GOLD_INGOT} items.
 * @param goldBlock   All {@link Items#GOLD_BLOCK} items.
 * @param ironIngot   All {@link Items#IRON_INGOT} items.
 * @param head        All types of skull (player heads, mob skulls, etc...).
 * @param dye         All dye items.
 * @param enderPearl  All {@link Items#ENDER_PEARL} items.
 * @param woodenChest All wooden chests (both normal and trapped chests).
 */
public record RecipeIngredients(
    Ingredient redstone,
    Ingredient string,
    Ingredient leather,
    Ingredient stone,
    Ingredient glassPane,
    Ingredient goldIngot,
    Ingredient goldBlock,
    Ingredient ironIngot,
    Ingredient head,
    Ingredient dye,
    Ingredient enderPearl,
    Ingredient woodenChest
) {
}
