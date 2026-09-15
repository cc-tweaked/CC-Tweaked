// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleRefuelHandler;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.OptionalInt;

public final class FurnaceRefuelHandler implements TurtleRefuelHandler {
    @Override
    public OptionalInt refuel(ITurtleAccess turtle, ItemStack currentStack, int slot, int limit) {
        var fuelPerItem = getFuelPerItem(turtle, currentStack);
        if (fuelPerItem <= 0) return OptionalInt.empty();
        if (limit == 0) return OptionalInt.of(0);

        var fuelSpaceLeft = turtle.getFuelLimit() - turtle.getFuelLevel();
        var fuelItemLimit = (int) Math.ceil(fuelSpaceLeft / (double) fuelPerItem);
        if (limit > fuelItemLimit) limit = fuelItemLimit;

        var stack = turtle.getInventory().removeItem(slot, limit);
        var fuelToGive = fuelPerItem * stack.getCount();
        // Store the replacement item in the inventory
        var replacementStack = PlatformHelper.get().getCraftingRemainingItem(stack);
        if (replacementStack != null) TurtleUtil.storeItemOrDrop(turtle, replacementStack.create());

        turtle.getInventory().setChanged();

        return OptionalInt.of(fuelToGive);
    }

    private static int getFuelPerItem(ITurtleAccess turtle, ItemStack stack) {
        // TODO(26.3): Find a cleaner way of accessing the turtle.
        var blockEntity = ((TurtleBrain) turtle).getOwner();
        var lootContext = new LootContext.Builder(
            new LootParams.Builder((ServerLevel) turtle.getLevel())
                .withParameter(LootContextParams.BLOCK_STATE, blockEntity.getBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(turtle.getPosition()))
                .withParameter(LootContextParams.CONTAINER, blockEntity)
                .create(LootContextParamSets.CONTAINER_PROCESS)
        ).create(Optional.empty());
        var burnTime = ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, lootContext, 0);
        return (burnTime * 5) / 100;
    }
}
