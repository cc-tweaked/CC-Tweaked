// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.pocket.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.shared.pocket.core.PocketComputerInternal;
import dan200.computercraft.shared.pocket.core.PocketSide;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

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
 * <p>
 * ## Recipes
 * <div class="recipe-container">
 *  <mc-recipe recipe="computercraft:pocket_computer_normal"></mc-recipe>
 *  <mc-recipe recipe="computercraft:pocket_computer_advanced"></mc-recipe>
 * </div>
 *
 * @cc.module pocket
 */
public class PocketAPI implements ILuaAPI {
    private final PocketComputerInternal pocket;

    public PocketAPI(PocketComputerInternal pocket) {
        this.pocket = pocket;
    }

    @Override
    public String[] getNames() {
        return new String[]{ "pocket" };
    }

    /**
     * Search the player's inventory for another upgrade, replacing the existing back upgrade with that item if found.
     * <p>
     * This inventory search starts from the player's currently selected slot, allowing you to prioritise upgrades.
     *
     * @return The result of equipping.
     * @cc.treturn boolean If an item was equipped.
     * @cc.treturn string|nil The reason an item was not equipped.
     */
    @LuaFunction(mainThread = true)
    public final Object[] equipBack() {
        return equip(PocketSide.BACK);
    }

    /**
     * Search the player's inventory for another upgrade, replacing the existing bottom upgrade with that item if found.
     * <p>
     * This inventory search starts from the player's currently selected slot, allowing you to prioritise upgrades.
     *
     * @return The result of equipping.
     * @cc.treturn boolean If an item was equipped.
     * @cc.treturn string|nil The reason an item was not equipped.
     * @since 1.116.0
     */
    @LuaFunction(mainThread = true)
    public final Object[] equipBottom() {
        return equip(PocketSide.BOTTOM);
    }

    private Object[] equip(PocketSide side) {
        var entity = pocket.getEntity();
        if (!(entity instanceof Player player)) return new Object[]{ false, "Cannot find player" };

        var inventory = player.getInventory();
        var previousUpgrade = pocket.getUpgrade(side);

        // Attempt to find the upgrade, starting in the main segment, and then looking in the opposite
        // one. We start from the position the item is currently in and loop round to the start.
        UpgradeData<IPocketUpgrade> newUpgrade = null;
        for (var i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            newUpgrade = findUpgrade(inventory, (i + inventory.getSelectedSlot()) % Inventory.INVENTORY_SIZE, previousUpgrade);
            if (newUpgrade != null) break;
        }
        if (newUpgrade == null) newUpgrade = findUpgrade(inventory, Inventory.SLOT_OFFHAND, previousUpgrade);
        if (newUpgrade == null) return new Object[]{ false, "Cannot find a valid upgrade" };

        // Remove the current upgrade
        if (previousUpgrade != null) storeItem(player, previousUpgrade.getUpgradeItem().create());

        // Set the new upgrade
        pocket.setUpgrade(side, newUpgrade);

        return new Object[]{ true };
    }

    /**
     * Remove the pocket computer's back upgrade.
     *
     * @return The result of unequipping.
     * @cc.treturn boolean If the upgrade was unequipped.
     * @cc.treturn string|nil The reason an upgrade was not unequipped.
     */
    @LuaFunction(mainThread = true)
    public final Object[] unequipBack() {
        return unequip(PocketSide.BACK);
    }

    /**
     * Remove the pocket computer's bottom upgrade.
     *
     * @return The result of unequipping.
     * @cc.treturn boolean If the upgrade was unequipped.
     * @cc.treturn string|nil The reason an upgrade was not unequipped.
     * @since 1.116.0
     */
    @LuaFunction(mainThread = true)
    public final Object[] unequipBottom() {
        return unequip(PocketSide.BOTTOM);
    }

    private Object[] unequip(PocketSide side) {
        var entity = pocket.getEntity();
        if (!(entity instanceof Player player)) return new Object[]{ false, "Cannot find player" };

        var previousUpgrade = pocket.getUpgrade(side);
        if (previousUpgrade == null) return new Object[]{ false, "Nothing to unequip" };

        pocket.setUpgrade(side, null);

        storeItem(player, previousUpgrade.getUpgradeItem().create());

        return new Object[]{ true };
    }

    private static void storeItem(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            var drop = player.drop(stack, false);
            if (drop != null) drop.setNoPickUpDelay();
        }
    }

    private @Nullable UpgradeData<IPocketUpgrade> findUpgrade(Container inv, int slot, @Nullable UpgradeData<IPocketUpgrade> previous) {
        var invStack = inv.getItem(slot);
        if (invStack.isEmpty()) return null;

        var newUpgrade = PocketUpgrades.instance().get(pocket.getLevel().registryAccess(), invStack);
        if (newUpgrade != null && !Objects.equals(newUpgrade, previous)) {
            // Consume an item from this stack and exit the loop
            invStack = invStack.copy();
            invStack.shrink(1);
            inv.setItem(slot, invStack.isEmpty() ? ItemStack.EMPTY : invStack);

            return newUpgrade;
        }

        return null;
    }
}
