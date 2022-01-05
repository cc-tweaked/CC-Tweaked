/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.pocket;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Wrapper class for pocket computers.
 */
public interface IPocketAccess
{
    /**
     * Gets the entity holding this item.
     *
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
    void setColour( int colour );

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
    void setLight( int colour );

    /**
     * Get the upgrade-specific NBT.
     *
     * This is persisted between computer reboots and chunk loads.
     *
     * @return The upgrade's NBT.
     * @see #updateUpgradeNBTData()
     */
    @Nonnull
    CompoundTag getUpgradeNBTData();

    /**
     * Mark the upgrade-specific NBT as dirty.
     *
     * @see #getUpgradeNBTData()
     */
    void updateUpgradeNBTData();

    /**
     * Remove the current peripheral and create a new one. You may wish to do this if the methods available change.
     */
    void invalidatePeripheral();

    /**
     * Get a list of all upgrades for the pocket computer.
     *
     * @return A collection of all upgrade names.
     */
    @Nonnull
    Map<ResourceLocation, IPeripheral> getUpgrades();
}
