// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.upgrades.MutableUpgradeHandle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * A pocket computer.
 *
 * @see IPocketAccess
 * @see dan200.computercraft.api.component.ComputerComponents#POCKET
 */
@ApiStatus.NonExtendable
public interface PocketComputer {
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
     * Get whether this pocket computer is "active".
     * <p>
     * Some operations on pocket computer (for instance, {@linkplain IPocketAccess#getUpgrade() reading and writing
     * upgrade data}) requires access to the stack the pocket computer is attached to. However, in some cases, the stack
     * may no longer be present (for instance, if it has been moved into a chest), and so those operations will fail.
     * <p>
     * In this case, the pocket computer is considered inactive: the computer will continue to run for several seconds,
     * but upgrades will not be {@linkplain IPocketUpgrade#update(IPocketAccess, MutableUpgradeHandle, IPeripheral)}
     * ticked}, and pocket computer data cannot be interacted with.
     * <p>
     * This function is not thread safe and must be called from the server thread.
     *
     * @return Whether this pocket computer is active.
     */
    boolean isActive();

    /**
     * Get the colour of this pocket computer as an RGB number.
     *
     * <p>
     * This method can only be called from the main server thread, when this computer is {@linkplain #isActive() is
     * active}.
     *
     * @return The colour this pocket computer is. This will be a RGB colour between {@code 0x000000} and
     * {@code 0xFFFFFF} or -1 if it has no colour.
     * @see #setColour(int)
     */
    int getColour();

    /**
     * Set the colour of the pocket computer to an RGB number.
     * <p>
     * This method can only be called from the main server thread, when this computer is {@linkplain #isActive() is
     * active}.
     *
     * @param colour The colour this pocket computer should be changed to. This should be a RGB colour between
     *               {@code 0x000000} and {@code 0xFFFFFF} or -1 to reset to the default colour.
     * @see #getColour()
     */
    void setColour(int colour);
}
