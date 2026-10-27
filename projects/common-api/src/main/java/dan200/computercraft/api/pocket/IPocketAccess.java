// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import dan200.computercraft.api.upgrades.UpgradeHandle;
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
     * Get a handle to the equipped upgrade.
     * <p>
     * This function is not thread safe and must be called from the server thread.
     *
     * @return The currently equipped upgrade.
     * @since 1.121.0
     */
    @Nullable UpgradeHandle<IPocketUpgrade> getUpgradeHandle();

    /**
     * Remove the current peripheral and create a new one.
     * <p>
     * You may wish to do this if the methods available change, for instance when the {@linkplain #getEntity() owning
     * entity} changes.
     */
    void invalidatePeripheral();
}
