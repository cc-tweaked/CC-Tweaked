// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.pocket.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.PocketUpgrades;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Control the current pocket computer, adding or removing upgrades.
 * <p>
 * This API is only available on pocket computers. As such, you may use its presence to determine what kind of computer
 * you are using:
 *
 * <pre>{@code
 * if pocket then
 *   print("On a pocket computer")
 * else
 *   print("On something else")
 * end
 * }</pre>
 *
 * @cc.module pocket
 */
public class PocketAPI implements ILuaAPI {
    private final IPocketAccess pocket;

    public PocketAPI(IPocketAccess pocket) {
        this.pocket = pocket;
    }

    @Override
    public String[] getNames() {
        return new String[]{ "pocket" };
    }

    /**
     * Search the player's inventory for another upgrade, replacing the existing one with that item if found.
     * <p>
     * This inventory search starts from the player's currently selected slot, allowing you to prioritise upgrades.
     *
     * @return The result of equipping.
     * @cc.treturn boolean If an item was equipped.
     * @cc.treturn string|nil The reason an item was not equipped.
     */
    @LuaFunction(mainThread = true)
    public final Object[] equipBack() {
        var entity = pocket.getEntity();
        if (!(entity instanceof Player player)) return new Object[]{ false, "Cannot find player" };
        var inventory = player.getInventory();
        var previousUpgrade = pocket.getUpgrade();

        // Attempt to find the upgrade, starting in the main segment, and then looking in the opposite
        // one. We start from the position the item is currently in and loop round to the start.
        var newUpgrade = findUpgrade(inventory.items, inventory.selected, previousUpgrade);
        if (newUpgrade == null) {
            newUpgrade = findUpgrade(inventory.offhand, 0, previousUpgrade);
        }
        if (newUpgrade == null) return new Object[]{ false, "Cannot find a valid upgrade" };

        // Remove the current upgrade
        if (previousUpgrade != null) storeItem(player, previousUpgrade.getUpgradeItem());

        // Set the new upgrade
        pocket.setUpgrade(newUpgrade);

        return new Object[]{ true };
    }

    /**
     * Remove the pocket computer's current upgrade.
     *
     * @return The result of unequipping.
     * @cc.treturn boolean If the upgrade was unequipped.
     * @cc.treturn string|nil The reason an upgrade was not unequipped.
     */
    @LuaFunction(mainThread = true)
    public final Object[] unequipBack() {
        var entity = pocket.getEntity();
        if (!(entity instanceof Player player)) return new Object[]{ false, "Cannot find player" };
        var previousUpgrade = pocket.getUpgrade();

        if (previousUpgrade == null) return new Object[]{ false, "Nothing to unequip" };

        pocket.setUpgrade(null);

        storeItem(player, previousUpgrade.getUpgradeItem());

        return new Object[]{ true };
    }

    private static void storeItem(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            var drop = player.drop(stack, false);
            if (drop != null) drop.setNoPickUpDelay();
        }
    }

    private static @Nullable UpgradeData<IPocketUpgrade> findUpgrade(NonNullList<ItemStack> inv, int start, @Nullable UpgradeData<IPocketUpgrade> previous) {
        for (var i = 0; i < inv.size(); i++) {
            var invStack = inv.get((i + start) % inv.size());
            if (!invStack.isEmpty()) {
                var newUpgrade = PocketUpgrades.instance().get(invStack);

                if (newUpgrade != null && !Objects.equals(newUpgrade, previous)) {
                    // Consume an item from this stack and exit the loop
                    invStack = invStack.copy();
                    invStack.shrink(1);
                    inv.set((i + start) % inv.size(), invStack.isEmpty() ? ItemStack.EMPTY : invStack);

                    return newUpgrade;
                }
            }
        }

        return null;
    }
}
