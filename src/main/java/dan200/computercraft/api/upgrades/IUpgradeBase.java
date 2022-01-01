/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Common functionality between {@link ITurtleUpgrade} and {@link IPocketUpgrade}.
 */
public interface IUpgradeBase
{
    /**
     * Gets a unique identifier representing this type of turtle upgrade. eg: "computercraft:wireless_modem"
     * or "my_mod:my_upgrade".
     *
     * You should use a unique resource domain to ensure this upgrade is uniquely identified.
     * The upgrade will fail registration if an already used ID is specified.
     *
     * @return The unique ID for this upgrade.
     */
    @Nonnull
    ResourceLocation getUpgradeID();

    /**
     * Return an unlocalised string to describe this type of computer in item names.
     *
     * Examples of built-in adjectives are "Wireless", "Mining" and "Crafty".
     *
     * @return The localisation key for this upgrade's adjective.
     */
    @Nonnull
    String getUnlocalisedAdjective();

    /**
     * Return an item stack representing the type of item that a computer must be crafted
     * with to create a version which holds this upgrade. This item stack is also used
     * to determine the upgrade given by {@code turtle.equipLeft()} or {@code pocket.equipBack()}
     *
     * This should be constant over a session (or at least a datapack reload). It is recommended
     * that you cache the stack too, in order to prevent constructing it every time the method
     * is called.
     *
     * @return The item stack to craft with, or {@link ItemStack#EMPTY} if it cannot be crafted.
     */
    @Nonnull
    ItemStack getCraftingItem();

    /**
     * Determine if an item is suitable for being used for this upgrade.
     *
     * When un-equipping an upgrade, we return {@link #getCraftingItem()} rather than
     * the original stack. In order to prevent people losing items with enchantments (or
     * repairing items with non-0 damage), we impose additional checks on the item.
     *
     * The default check requires that any non-capability NBT is exactly the same as the
     * crafting item, but this may be relaxed for your upgrade.
     *
     * @param stack The stack to check. This is guaranteed to be non-empty and have the same item as
     *              {@link #getCraftingItem()}.
     * @return If this stack may be used to equip this upgrade.
     * @see net.minecraftforge.common.crafting.NBTIngredient#test(ItemStack) For the implementation of the default
     * check.
     */
    default boolean isItemSuitable( @Nonnull ItemStack stack )
    {
        ItemStack crafting = getCraftingItem();

        // A more expanded form of ItemStack.areShareTagsEqual, but allowing an empty tag to be equal to a
        // null one.
        CompoundTag shareTag = stack.getItem().getShareTag( stack );
        CompoundTag craftingShareTag = crafting.getItem().getShareTag( crafting );
        if( shareTag == craftingShareTag ) return true;
        if( shareTag == null ) return craftingShareTag.isEmpty();
        if( craftingShareTag == null ) return shareTag.isEmpty();
        return shareTag.equals( craftingShareTag );
    }

    /**
     * Get a suitable default unlocalised adjective for an upgrade ID. This converts "modid:some_upgrade" to
     * "upgrade.modid.some_upgrade.adjective".
     *
     * @param id The upgrade ID.
     * @return The  generated adjective.
     * @see #getUnlocalisedAdjective()
     */
    @Nonnull
    static String getDefaultAdjective( @Nonnull ResourceLocation id )
    {
        return Util.makeDescriptionId( "upgrade", id ) + ".adjective";
    }
}
