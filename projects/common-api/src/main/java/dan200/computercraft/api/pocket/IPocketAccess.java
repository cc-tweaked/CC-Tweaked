// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Wrapper class for pocket computers.
 */
public interface IPocketAccess {
    /**
     * Gets the entity holding this item.
     * <p>
     * This must be called on the server thread.
     *
     * @return The holding entity, or {@code null} if none exists.
     */
    @Nullable
    Entity getEntity();

    /**
     * Get the colour of this pocket computer as a RGB number.
     *
     * @return The colour this pocket computer is. This will be a RGB colour between {@code 0x000000} and
     * {@code 0xFFFFFF} or -1 if it has no colour.
     * @see #setColour(int)
     */
    int getColour();

    /**
     * Set the colour of the pocket computer to a RGB number.
     *
     * @param colour The colour this pocket computer should be changed to. This should be a RGB colour between
     *               {@code 0x000000} and {@code 0xFFFFFF} or -1 to reset to the default colour.
     * @see #getColour()
     */
    void setColour(int colour);

    /**
     * Get the colour of this pocket computer's light as a RGB number.
     *
     * @return The colour this light is. This will be a RGB colour between {@code 0x000000} and {@code 0xFFFFFF} or
     * -1 if it has no colour.
     * @see #setLight(int)
     */
    int getLight();

    /**
     * Set the colour of the pocket computer's light to a RGB number.
     *
     * @param colour The colour this modem's light will be changed to. This should be a RGB colour between
     *               {@code 0x000000} and {@code 0xFFFFFF} or -1 to reset to the default colour.
     * @see #getLight()
     */
    void setLight(int colour);

    /**
     * Get the upgrade-specific NBT.
     * <p>
     * This is persisted between computer reboots and chunk loads.
     *
     * @return The upgrade's NBT.
     * @see #updateUpgradeNBTData()
     * @see UpgradeBase#getUpgradeItem(CompoundTag)
     * @see UpgradeBase#getUpgradeData(ItemStack)
     */
    CompoundTag getUpgradeNBTData();

    /**
     * Mark the upgrade-specific NBT as dirty.
     *
     * @see #getUpgradeNBTData()
     */
    void updateUpgradeNBTData();

    /**
     * Remove the current peripheral and create a new one.
     * <p>
     * You may wish to do this if the methods available change, for instance when the {@linkplain #getEntity() owning
     * entity} changes.
     */
    void invalidatePeripheral();
}
