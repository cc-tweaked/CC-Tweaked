// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.impl.PlatformHelper;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Objects;

/**
 * Common functionality between {@link ITurtleUpgrade} and {@link IPocketUpgrade}.
 */
public interface UpgradeBase {
    /**
     * Gets a unique identifier representing this type of turtle upgrade. eg: "computercraft:wireless_modem"
     * or "my_mod:my_upgrade".
     * <p>
     * You should use a unique resource domain to ensure this upgrade is uniquely identified.
     * The upgrade will fail registration if an already used ID is specified.
     *
     * @return The unique ID for this upgrade.
     */
    ResourceLocation getUpgradeID();

    /**
     * Return an unlocalised string to describe this type of computer in item names.
     * <p>
     * Examples of built-in adjectives are "Wireless", "Mining" and "Crafty".
     *
     * @return The localisation key for this upgrade's adjective.
     */
    String getUnlocalisedAdjective();

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
     * @deprecated Prefer {@link #getCraftingIngredient()}.
     */
    @Deprecated(since = "1.121.0")
    ItemStack getCraftingItem();

    /**
     * Returns an ingredient representing the item(s) that a computer must be crafted with to create a version which
     * holds this upgrade.
     * <p>
     * This should be constant over a session (or at least a datapack reload).
     *
     * @return The ingredient for this upgrade.
     */
    default Ingredient getCraftingIngredient() {
        return Ingredient.of(getCraftingItem());
    }

    /**
     * Returns the item stack representing a currently equipped turtle upgrade.
     * <p>
     * Historically, upgrades did not store the item they were equipped with. Instead, {@link #getUpgradeData(ItemStack)}
     * could be used to extract data from the equipped item, then {@link #getUpgradeItem(CompoundTag)} could be used to
     * reconstruct it.
     * <p>
     * As of CC: Tweaked 1.121.0, upgrades now store the item they were originally equipped with. This function is
     * used to reconstruct the item from older saves, or if the upgrade still uses the legacy
     * {@link ITurtleAccess#updateUpgradeNBTData(TurtleSide)}/{@link IPocketAccess#updateUpgradeNBTData()} API.
     *
     * @param upgradeData The current upgrade data. This should <strong>NOT</strong> be mutated.
     * @return The item stack returned when unequipping.
     */
    default ItemStack getUpgradeItem(CompoundTag upgradeData) {
        return getCraftingItem();
    }

    /**
     * Extract upgrade data from an {@link ItemStack}.
     * <p>
     * This upgrade data will be available with {@link UpgradeHandle#getData()} or
     * {@link IPocketAccess#getUpgradeNBTData()}.
     * <p>
     * This should be an inverse to {@link #getUpgradeItem(CompoundTag)}.
     *
     * @param stack The stack that was equipped by the turtle or pocket computer. This will have the same item as
     *              {@link #getCraftingItem()}.
     * @return The upgrade data that should be set on the turtle or pocket computer.
     * @deprecated Upgrades now store their item by default, so this function does not need to be implemented.
     */
    @Deprecated(since = "1.121.0")
    default CompoundTag getUpgradeData(ItemStack stack) {
        return new CompoundTag();
    }

    /**
     * Determine if an item is suitable for being used for this upgrade.
     * <p>
     * When un-equipping an upgrade, we return {@link #getCraftingItem()} rather than
     * the original stack. In order to prevent people losing items with enchantments (or
     * repairing items with non-0 damage), we impose additional checks on the item.
     * <p>
     * The default check requires that any non-capability NBT is exactly the same as the
     * crafting item, but this may be relaxed for your upgrade.
     * <p>
     * This is based on {@code net.minecraftforge.common.crafting.StrictNBTIngredient}'s check.
     *
     * @param stack The stack to check. This is guaranteed to be non-empty and have the same item as
     *              {@link #getCraftingItem()}.
     * @return If this stack may be used to equip this upgrade.
     */
    default boolean isItemSuitable(ItemStack stack) {
        var crafting = getCraftingItem();

        // A more expanded form of ItemStack.areShareTagsEqual, but allowing an empty tag to be equal to a
        // null one.
        var shareTag = PlatformHelper.get().getShareTag(stack);
        var craftingShareTag = PlatformHelper.get().getShareTag(crafting);
        if (shareTag == craftingShareTag) return true;
        if (shareTag == null) return Objects.requireNonNull(craftingShareTag).isEmpty();
        if (craftingShareTag == null) return shareTag.isEmpty();
        return shareTag.equals(craftingShareTag);
    }

    /**
     * Get a suitable default unlocalised adjective for an upgrade ID. This converts "modid:some_upgrade" to
     * "upgrade.modid.some_upgrade.adjective".
     *
     * @param id The upgrade ID.
     * @return The  generated adjective.
     * @see #getUnlocalisedAdjective()
     */
    static String getDefaultAdjective(ResourceLocation id) {
        return Util.makeDescriptionId("upgrade", id) + ".adjective";
    }
}
