/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.test.shared.WithMinecraft;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static dan200.computercraft.test.shared.ItemStackMatcher.isStack;
import static org.hamcrest.MatcherAssert.assertThat;

@WithMinecraft
public class InventoryUtilTest {
    @Test
    public void testStoreOffset() {
        var container = new SimpleContainer(9);

        var remainder = InventoryUtil.storeItemsFromOffset(container, new ItemStack(Items.COBBLESTONE, 32), 4);
        assertThat("Remainder is empty", remainder, isStack(ItemStack.EMPTY));
        assertThat("Was inserted into slot", container.getItem(4), isStack(new ItemStack(Items.COBBLESTONE, 32)));
    }

    @Test
    public void testStoreOffsetWraps() {
        var container = new SimpleContainer(9);
        container.setItem(0, new ItemStack(Items.DIRT));
        for (var slot = 4; slot < 9; slot++) container.setItem(slot, new ItemStack(Items.DIRT));

        var remainder = InventoryUtil.storeItemsFromOffset(container, new ItemStack(Items.COBBLESTONE, 32), 4);
        assertThat("Remainder is empty", remainder, isStack(ItemStack.EMPTY));
        assertThat("Was inserted into slot", container.getItem(1), isStack(new ItemStack(Items.COBBLESTONE, 32)));
    }
}
