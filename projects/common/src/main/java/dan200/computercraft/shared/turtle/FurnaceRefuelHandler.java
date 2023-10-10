// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleRefuelHandler;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;

public final class FurnaceRefuelHandler implements TurtleRefuelHandler {
    @Override
    public OptionalInt refuel(ITurtleAccess turtle, ItemStack currentStack, int slot, int limit) {
        var fuelPerItem = getFuelPerItem(currentStack);
        if (fuelPerItem <= 0) return OptionalInt.empty();
        if (limit == 0) return OptionalInt.of(0);

        var fuelSpaceLeft = turtle.getFuelLimit() - turtle.getFuelLevel();
        var fuelItemLimit = (int) Math.ceil(fuelSpaceLeft / (double) fuelPerItem);
        if (limit > fuelItemLimit) limit = fuelItemLimit;

        var stack = turtle.getInventory().removeItem(slot, limit);
        var fuelToGive = fuelPerItem * stack.getCount();
        // Store the replacement item in the inventory
        var replacementStack = PlatformHelper.get().getCraftingRemainingItem(stack);
        if (!replacementStack.isEmpty()) TurtleUtil.storeItemOrDrop(turtle, replacementStack);

        turtle.getInventory().setChanged();

        return OptionalInt.of(fuelToGive);
    }

    private static int getFuelPerItem(ItemStack stack) {
        return (PlatformHelper.get().getBurnTime(stack) * 5) / 100;
    }
}
