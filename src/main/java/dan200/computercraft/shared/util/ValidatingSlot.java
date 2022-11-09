/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class ValidatingSlot extends Slot {
    private final Predicate<ItemStack> predicate;

    public ValidatingSlot(Container inventoryIn, int index, int xPosition, int yPosition, Predicate<ItemStack> predicate) {
        super(inventoryIn, index, xPosition, yPosition);
        this.predicate = predicate;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return predicate.test(stack);
    }
}
