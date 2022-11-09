/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.test.shared.platform;

import dan200.computercraft.shared.platform.ContainerTransfer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static dan200.computercraft.test.shared.ItemStackMatcher.isStack;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Interface defining the behaviour of a {@link ContainerTransfer} implementation.
 */
public interface ContainerTransferContract {
    ContainerTransfer.Slotted wrap(Container container);

    @Test
    default void testMoveSameInventorySlot() {
        var inv = new SimpleContainer(4);
        inv.setItem(0, new ItemStack(Items.DIRT, 64));

        var move = wrap(inv).singleSlot(0).moveTo(wrap(inv).singleSlot(0), 64);
        assertEquals(ContainerTransfer.NO_SPACE, move);

        assertThat(inv.getItem(0), isStack(new ItemStack(Items.DIRT, 64)));

        assertNoOverlap(inv);
    }

    @Test
    default void testMoveSameInventory() {
        var inv = new SimpleContainer(4);
        inv.setItem(0, new ItemStack(Items.DIRT, 64));

        var move = wrap(inv).singleSlot(0).moveTo(wrap(inv).singleSlot(1), 64);
        assertEquals(64, move);

        assertThat(inv.getItem(0), isStack(ItemStack.EMPTY));
        assertThat(inv.getItem(1), isStack(new ItemStack(Items.DIRT, 64)));

        assertNoOverlap(inv);
    }

    @Test
    default void testMoveDifferentInventories() {
        var destination = new SimpleContainer(4);

        var source = new SimpleContainer(4);
        source.setItem(0, new ItemStack(Items.DIRT, 64));

        var move = wrap(source).moveTo(wrap(destination), 64);
        assertEquals(64, move);

        assertThat(source.getItem(0), isStack(ItemStack.EMPTY));
        assertThat(destination.getItem(0), isStack(new ItemStack(Items.DIRT, 64)));

        assertNoOverlap(source, destination);
    }

    @Test
    default void testMoveDistributeDestination() {
        var destination = new SimpleContainer(4);
        destination.setItem(0, new ItemStack(Items.DIRT, 48));
        destination.setItem(1, new ItemStack(Items.DIRT, 48));
        destination.setItem(2, new ItemStack(Items.DIRT, 48));
        destination.setItem(3, new ItemStack(Items.DIRT, 48));

        var source = new SimpleContainer(4);
        source.setItem(0, new ItemStack(Items.DIRT, 64));

        var move = wrap(source).moveTo(wrap(destination), 64);
        assertEquals(64, move);

        assertThat(source.getItem(0), isStack(ItemStack.EMPTY));
        for (var i = 0; i < 4; i++) {
            assertThat("Stack in slot " + i, destination.getItem(i), isStack(new ItemStack(Items.DIRT, 64)));
        }

        assertNoOverlap(source, destination);
    }

    @Test
    default void testMoveSkipFullSlot() {
        var destination = new SimpleContainer(4);
        destination.setItem(0, new ItemStack(Items.DIRT, 64));

        var source = new SimpleContainer(4);
        source.setItem(0, new ItemStack(Items.DIRT, 64));

        var move = wrap(source).moveTo(wrap(destination), 64);
        assertEquals(64, move);

        assertThat(source.getItem(1), isStack(ItemStack.EMPTY));
        assertThat(destination.getItem(0), isStack(new ItemStack(Items.DIRT, 64)));
        assertThat(destination.getItem(1), isStack(new ItemStack(Items.DIRT, 64)));

        assertNoOverlap(source, destination);
    }

    static void assertNoOverlap(Container... containers) {
        Set<ItemStack> stacks = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var container : containers) {
            for (var slot = 0; slot < container.getContainerSize(); slot++) {
                var item = container.getItem(slot);
                if (item == ItemStack.EMPTY) continue;

                if (!stacks.add(item)) throw new AssertionError("Duplicate item in inventories");
            }
        }
    }
}
