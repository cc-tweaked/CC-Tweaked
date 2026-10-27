// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.impl.UpgradeData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An internal version of {@link ITurtleAccess}.
 * <p>
 * This exposes additional functionality we don't want in the public API, but where we don't want access to the full
 * {@link TurtleBrain} interface.
 */
public interface TurtleAccessInternal extends ITurtleAccess {
    /**
     * Get an immutable snapshot of an item in the inventory. This is a thread-safe version of
     * {@code getInventory().getItem()}.
     *
     * @param slot The slot
     * @return The current item. This should NOT be modified.
     * @see net.minecraft.world.Container#getItem(int)
     */
    ItemStack getItemSnapshot(int slot);

    @Nullable UpgradeData<ITurtleUpgrade> getUpgradeWithData(TurtleSide side);

    /**
     * Set the upgrade for a given side and its upgrade data.
     *
     * @param side    The side to set the upgrade on.
     * @param upgrade The upgrade to set, may be {@code null} to clear.
     */
    void setUpgrade(TurtleSide side, @Nullable UpgradeData<ITurtleUpgrade> upgrade);
}
