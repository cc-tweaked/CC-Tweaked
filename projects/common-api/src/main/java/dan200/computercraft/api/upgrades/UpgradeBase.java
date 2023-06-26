// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.impl.PlatformHelper;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
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
     */
    ItemStack getCraftingItem();

    /**
     * Return an item stack representing the instance of upgrade with passed upgrade data.
     * By default, this method output equals to {@link #getCraftingItem()} because default behavior of
     * upgrades is ignored any upgrade data.
     * <p>
     * But if your upgrade required this, you can redefine this behavior and pass some information from upgrade data
     * to item stack. Please, use this method with cautions.
     * <p>
     * This method will be used by {@code turtle.unequipLeft()} and {@code pocket.unequipBack()} to determine item stack
     * that should be placed inside inventory.
     *
     * @param upgradeData NBT information that was stored inside turtle/pocket by upgrade
     * @return The item stack, that should be stored inside inventory
     */
    default @Nonnull ItemStack getUpgradeItem(@Nonnull CompoundTag upgradeData) {
        return getCraftingItem();
    }

    /**
     * Returns initial upgrade data, that should be set for turtle/pocket, that uses this upgrade, based on item stack
     * that used for installing upgrade right now.
     * @param stack Item Stack that used for upgrade
     * @return Upgrade NBT data that should be set in time of adding this upgrade to turtle/pocket
     */
    default @Nonnull CompoundTag getUpgradeData(@Nonnull ItemStack stack) {
        return new CompoundTag();
    }

    /**
     * Specific hook, that allow you to filter data, that should be stored when turtle was broken.
     * Use this in cases when you don't need to store all upgrade data by default, and you want to change this
     * behavior, for example, as modem do.
     * @param upgradeData NBT data that currently stored for this upgrade
     * @return filtered version of this data, by default just all of it
     */
    default @Nonnull CompoundTag getPersistedData(CompoundTag upgradeData) {
        return upgradeData;
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
