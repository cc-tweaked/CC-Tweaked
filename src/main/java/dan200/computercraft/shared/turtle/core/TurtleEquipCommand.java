/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import javax.annotation.Nonnull;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.ItemStorage;
import dan200.computercraft.shared.util.WorldUtil;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class TurtleEquipCommand implements ITurtleCommand {
    private final TurtleSide m_side;

    public TurtleEquipCommand(TurtleSide side) {
        this.m_side = side;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        // Determine the upgrade to equipLeft
        ITurtleUpgrade newUpgrade;
        ItemStack newUpgradeStack;
        Inventory inventory = turtle.getInventory();
        ItemStorage storage = ItemStorage.wrap(turtle.getInventory());
        ItemStack selectedStack = inventory.getStack(turtle.getSelectedSlot());
        if (!selectedStack.isEmpty()) {
            newUpgradeStack = selectedStack.copy();
            newUpgrade = TurtleUpgrades.get(newUpgradeStack);
            if (newUpgrade == null || !TurtleUpgrades.suitableForFamily(((TurtleBrain) turtle).getFamily(), newUpgrade)) {
                return TurtleCommandResult.failure("Not a valid upgrade");
            }
        } else {
            newUpgradeStack = null;
            newUpgrade = null;
        }

        // Determine the upgrade to replace
        ItemStack oldUpgradeStack;
        ITurtleUpgrade oldUpgrade = turtle.getUpgrade(this.m_side);
        if (oldUpgrade != null) {
            ItemStack craftingItem = oldUpgrade.getCraftingItem();
            oldUpgradeStack = !craftingItem.isEmpty() ? craftingItem.copy() : null;
        } else {
            oldUpgradeStack = null;
        }

        TurtleActionEvent event = new TurtleActionEvent(turtle, TurtleAction.EQUIP);
        if (TurtleEvent.post(event)) {
            return TurtleCommandResult.failure(event.getFailureMessage());
        }

        // Do the swapping:
        if (newUpgradeStack != null) {
            // Consume new upgrades item
            InventoryUtil.takeItems(1, storage, turtle.getSelectedSlot(), 1, turtle.getSelectedSlot());
        }
        if (oldUpgradeStack != null) {
            // Store old upgrades item
            ItemStack remainder = InventoryUtil.storeItems(oldUpgradeStack, storage, turtle.getSelectedSlot());
            if (!remainder.isEmpty()) {
                // If there's no room for the items, drop them
                BlockPos position = turtle.getPosition();
                WorldUtil.dropItemStack(remainder, turtle.getWorld(), position, turtle.getDirection());
            }
        }
        turtle.setUpgrade(this.m_side, newUpgrade);

        // Animate
        if (newUpgrade != null || oldUpgrade != null) {
            turtle.playAnimation(TurtleAnimation.Wait);
        }

        return TurtleCommandResult.success();
    }
}
