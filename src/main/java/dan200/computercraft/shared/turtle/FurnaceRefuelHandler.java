/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle;

import javax.annotation.Nonnull;

import com.google.common.eventbus.Subscribe;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.event.TurtleRefuelEvent;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.ItemStorage;
import dan200.computercraft.shared.util.WorldUtil;

import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class FurnaceRefuelHandler implements TurtleRefuelEvent.Handler {
    public static final FurnaceRefuelHandler INSTANCE = new FurnaceRefuelHandler();

    private FurnaceRefuelHandler() {
    }

    @Subscribe
    public static void onTurtleRefuel(TurtleRefuelEvent event) {
        if (event.getHandler() == null && getFuelPerItem(event.getStack()) > 0) {
            event.setHandler(INSTANCE);
        }
    }

    @Override
    public int refuel(@Nonnull ITurtleAccess turtle, @Nonnull ItemStack currentStack, int slot, int limit) {
        ItemStorage storage = ItemStorage.wrap(turtle.getInventory());
        ItemStack stack = storage.take(slot, limit, ItemStack.EMPTY, false);
        int fuelToGive = getFuelPerItem(stack) * stack.getCount();

        // Store the replacement item in the inventory
        Item replacementStack = stack.getItem()
                                     .getRecipeRemainder();
        if (replacementStack != null) {
            ItemStack remainder = InventoryUtil.storeItems(new ItemStack(replacementStack), storage, turtle.getSelectedSlot());
            if (!remainder.isEmpty()) {
                WorldUtil.dropItemStack(remainder,
                                        turtle.getWorld(),
                                        turtle.getPosition(),
                                        turtle.getDirection()
                                              .getOpposite());
            }
        }

        return fuelToGive;
    }

    private static int getFuelPerItem(@Nonnull ItemStack stack) {
        int burnTime = FurnaceBlockEntity.createFuelTimeMap()
                                         .getOrDefault(stack.getItem(), 0);
        return (burnTime * 5) / 100;
    }
}
