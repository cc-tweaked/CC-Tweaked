// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeData;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Access to a pocket computer for {@linkplain IPocketUpgrade pocket upgrades}.
 */
@ApiStatus.NonExtendable
public interface IPocketAccess extends PocketComputer {
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
     * @see #getUpgradeData()
     * @see #setUpgrade(UpgradeData)
     */
    @Nullable
    UpgradeData<IPocketUpgrade> getUpgrade();

    /**
     * Set the upgrade for this pocket computer, also updating the item stack.
     * <p>
     * This method can only be called from the main server thread, when this computer is {@linkplain #isActive() is
     * active}.
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
     * @see #setUpgradeData(DataComponentPatch)
     * @see UpgradeBase#getUpgradeItem(DataComponentPatch)
     * @see UpgradeBase#getUpgradeData(ItemStack)
     * @see #getUpgrade()
     */
    DataComponentPatch getUpgradeData();

    /**
     * Update the upgrade-specific data.
     * <p>
     * This method can only be called from the main server thread, when this computer is {@linkplain #isActive() is
     * active}.
     *
     * @param data The new upgrade data.
     * @see #getUpgradeData()
     */
    void setUpgradeData(DataComponentPatch data);

    /**
     * Remove the current peripheral and create a new one.
     * <p>
     * You may wish to do this if the methods available change, for instance when the {@linkplain #getEntity() owning
     * entity} changes.
     */
    void invalidatePeripheral();
}
