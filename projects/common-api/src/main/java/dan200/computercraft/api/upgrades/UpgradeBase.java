// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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
     * Return an item stack representing the type of item that a computer must be crafted
     * with to create a version which holds this upgrade. This item stack is also used
     * to determine the upgrade given by {@code turtle.equipLeft()} or {@code pocket.equipBack()}
     * <p>
     * This should be constant over a session (or at least a datapack reload). It is recommended
     * that you cache the stack too, in order to prevent constructing it every time the method
     * is called.
     *
     * @return The item stack to craft with, or {@link ItemStack#EMPTY} if it cannot be crafted.
     */
    ItemStack getCraftingItem();

    /**
     * Returns the item stack representing a currently equipped turtle upgrade.
     * <p>
     * While upgrades can store upgrade data ({@link ITurtleAccess#getUpgradeData(TurtleSide)} and
     * {@link IPocketAccess#getUpgradeData()}}, by default this data is discarded when an upgrade is unequipped,
     * and the original item stack is returned.
     * <p>
     * By overriding this method, you can create a new {@link ItemStack} which contains enough data to
     * {@linkplain #getUpgradeData(ItemStack) re-create the upgrade data} if the item is re-equipped.
     * <p>
     * When overriding this, you should override {@link #getUpgradeData(ItemStack)} and {@link #isItemSuitable(ItemStack)}
     * at the same time,
     *
     * @param upgradeData The current upgrade data. This should <strong>NOT</strong> be mutated.
     * @return The item stack returned when unequipping.
     */
    default ItemStack getUpgradeItem(DataComponentPatch upgradeData) {
        return getCraftingItem();
    }

    /**
     * Extract upgrade data from an {@link ItemStack}.
     * <p>
     * This upgrade data will be available with {@link ITurtleAccess#getUpgradeData(TurtleSide)} or
     * {@link IPocketAccess#getUpgradeData()}.
     * <p>
     * This should be an inverse to {@link #getUpgradeItem(DataComponentPatch)}.
     *
     * @param stack The stack that was equipped by the turtle or pocket computer. This will have the same item as
     *              {@link #getCraftingItem()}.
     * @return The upgrade data that should be set on the turtle or pocket computer.
     */
    default DataComponentPatch getUpgradeData(ItemStack stack) {
        return DataComponentPatch.EMPTY;
    }

    /**
     * Determine if an item is suitable for being used for this upgrade.
     * <p>
     * When un-equipping an upgrade, we return {@link #getCraftingItem()} rather than
     * the original stack. In order to prevent people losing items with enchantments (or
     * repairing items with non-0 damage), we impose additional checks on the item.
     * <p>
     * The default check requires that any NBT is exactly the same as the crafting item,
     * but this may be relaxed for your upgrade.
     *
     * @param stack The stack to check. This is guaranteed to be non-empty and have the same item as
     *              {@link #getCraftingItem()}.
     * @return If this stack may be used to equip this upgrade.
     */
    default boolean isItemSuitable(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(getCraftingItem(), stack);
    }

    /**
     * Get a suitable default unlocalised adjective for an upgrade ID. This converts "modid:some_upgrade" to
     * "upgrade.modid.some_upgrade.adjective".
     *
     * @param id The upgrade ID.
     * @return The  generated adjective.
     * @see #getAdjective()
     */
    static String getDefaultAdjective(ResourceLocation id) {
        return Util.makeDescriptionId("upgrade", id) + ".adjective";
    }
}
