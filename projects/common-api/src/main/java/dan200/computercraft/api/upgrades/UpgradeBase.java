// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Common functionality between {@link ITurtleUpgrade} and {@link IPocketUpgrade}.
 */
public interface UpgradeBase {
    /**
     * Get the type of this upgrade.
     *
     * @return The type of this upgrade.
     */
    UpgradeType<?> getType();

    /**
     * A description of this upgrade for use in item names.
     * <p>
     * This should typically be a {@linkplain Component#translatable(String) translation key}, rather than a hard coded
     * string.
     * <p>
     * Examples of built-in adjectives are "Wireless", "Mining" and "Crafty".
     *
     * @return The text component for this upgrade's adjective.
     */
    Component getAdjective();

    /**
     * Returns an ingredient representing the item(s) that a computer must be crafted with to create a version which
     * holds this upgrade.
     * <p>
     * This should be constant for a given upgrade.
     *
     * @return The ingredient for this upgrade.
     * @since 1.121.0
     */
    Ingredient getCraftingIngredient();

    /**
     * Returns the item stack representing a currently equipped turtle upgrade.
     * <p>
     * Historically, upgrades did not store the item they were equipped with. Instead, one could extract data from the
     * equipped item, then call {@link #getUpgradeItem(DataComponentPatch)} to reconstruct it.
     * <p>
     * As of CC: Tweaked 1.121.0, upgrades now store the item they were originally equipped with. This function is
     * used to reconstruct the item from older saves.
     *
     * @param upgradeData The current upgrade data.
     * @return The item provided by this upgrade.
     */
    default ItemStackTemplate getUpgradeItem(DataComponentPatch upgradeData) {
        var items = getCraftingIngredient().items().findFirst().orElseThrow(() -> new IllegalStateException("No item preset"));
        return new ItemStackTemplate(items.value());
    }

    /**
     * Determine if an item is suitable for being used for this upgrade.
     * <p>
     * This is used <em>in addition to</em> {@link #getCraftingIngredient()} to determine whether an item is suitable
     * as an upgrade. This may be used to check additional properties of an item (e.g. the presence/absence of a
     * component) that is not possible with vanilla's {@link Ingredient}s.
     *
     * @param stack The stack to check. This is guaranteed to be non-empty and match {@link #getCraftingIngredient()}.
     * @return If this stack may be used to equip this upgrade.
     */
    default boolean isItemSuitable(ItemStack stack) {
        return true;
    }

    /**
     * Get a suitable default unlocalised adjective for an upgrade ID. This converts "modid:some_upgrade" to
     * "upgrade.modid.some_upgrade.adjective".
     *
     * @param id The upgrade ID.
     * @return The  generated adjective.
     * @see #getAdjective()
     */
    static String getDefaultAdjective(Identifier id) {
        return Util.makeDescriptionId("upgrade", id) + ".adjective";
    }
}
