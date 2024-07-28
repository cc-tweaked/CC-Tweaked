// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Wrapper class for pocket computers.
 */
@ApiStatus.NonExtendable
public interface IPocketAccess {
    /**
     * Get the level in which the pocket computer exists.
     *
     * @return The pocket computer's level.
     */
    ServerLevel getLevel();

    /**
     * Get the position of the pocket computer.
     *
     * @return The pocket computer's position.
     */
    Vec3 getPosition();

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
     * Get the currently equipped upgrade.
     *
     * @return The currently equipped upgrade.
     * @see #getUpgradeNBTData()
     * @see #setUpgrade(UpgradeData)
     */
    @Nullable
    UpgradeData<IPocketUpgrade> getUpgrade();

    /**
     * Set the upgrade for this pocket computer, also updating the item stack.
     * <p>
     * Note this method is not thread safe - it must be called from the server thread.
     *
     * @param upgrade The new upgrade to set it to, may be {@code null}.
     * @see #getUpgrade()
     */
    void setUpgrade(@Nullable UpgradeData<IPocketUpgrade> upgrade);

    /**
     * Get the upgrade-specific NBT.
     * <p>
     * This is persisted between computer reboots and chunk loads.
     *
     * @return The upgrade's NBT.
     * @see #updateUpgradeNBTData()
     * @see UpgradeBase#getUpgradeItem(CompoundTag)
     * @see UpgradeBase#getUpgradeData(ItemStack)
     * @see #getUpgrade()
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

    /**
     * Get a list of all upgrades for the pocket computer.
     *
     * @return A collection of all upgrade names.
     * @deprecated This is a relic of a previous API, which no longer makes sense with newer versions of ComputerCraft.
     */
    @Deprecated(forRemoval = true)
    Map<ResourceLocation, IPeripheral> getUpgrades();
}
