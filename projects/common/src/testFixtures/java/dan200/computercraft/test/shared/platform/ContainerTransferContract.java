// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared.platform;

import dan200.computercraft.shared.platform.ContainerTransfer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

        assertThat(inv.getItem(0), isStack(Items.DIRT, 64));

        assertNoOverlap(inv);
    }

    @Test
    default void testMoveSameInventory() {
        var inv = new SimpleContainer(4);
        inv.setItem(0, new ItemStack(Items.DIRT, 64));

        var move = wrap(inv).singleSlot(0).moveTo(wrap(inv).singleSlot(1), 64);
        assertEquals(64, move);

        assertThat(inv.getItem(0), isStack(ItemStack.EMPTY));
        assertThat(inv.getItem(1), isStack(Items.DIRT, 64));

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
        assertThat(destination.getItem(0), isStack(Items.DIRT, 64));

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
            assertThat("Stack in slot " + i, destination.getItem(i), isStack(Items.DIRT, 64));
        }

        assertNoOverlap(source, destination);
    }

    @ParameterizedTest(name = "offset = {0}")
    @ValueSource(ints = { 0, 1, 2 })
    default void testMoveDistributeWithLimit(int offset) {
        // We create a specific setup here such that each slot has room for strictly less than the limit(=17).
        // There was a bug (https://github.com/cc-tweaked/CC-Tweaked/issues/1338) where this would insert items beyond
        // the limit as it did not account for the number of items already transferred.
        var destination = new SimpleContainer(4);
        destination.setItem(offset, new ItemStack(Items.DIRT, 48));
        destination.setItem((offset + 1) % 4, new ItemStack(Items.DIRT, 48));

        var source = new SimpleContainer(4);
        source.setItem(0, new ItemStack(Items.DIRT, 64));
        source.setItem(1, new ItemStack(Items.DIRT, 64));

        var move = wrap(source).moveTo(wrap(destination).rotate(offset), 17);
        assertEquals(17, move);

        assertThat("Source stack in slot 0", source.getItem(0), isStack(Items.DIRT, 47));
        assertThat("Source stack in slot 1", source.getItem(1), isStack(Items.DIRT, 64));

        assertThat("Dest stack in slot 0", destination.getItem(offset), isStack(Items.DIRT, 64));
        assertThat("Dest stack in slot 2", destination.getItem((offset + 1) % 4), isStack(Items.DIRT, 49));
        assertThat("Dest stack in slot 3", destination.getItem((offset + 2) % 4), isStack(ItemStack.EMPTY));
        assertThat("Dest stack in slot 3", destination.getItem((offset + 3) % 4), isStack(ItemStack.EMPTY));

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
        assertThat(destination.getItem(0), isStack(Items.DIRT, 64));
        assertThat(destination.getItem(1), isStack(Items.DIRT, 64));

        assertNoOverlap(source, destination);
    }

    @Test
    default void testNoMoveReject() {
        var destination = new SimpleContainer(4) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack item) {
                return false;
            }
        };

        var source = new SimpleContainer(4);
        source.setItem(0, new ItemStack(Items.DIRT, 64));

        var move = wrap(source).moveTo(wrap(destination), 64);
        assertEquals(ContainerTransfer.NO_SPACE, move);

        assertThat(source.getItem(0), isStack(Items.DIRT, 64));
        assertThat(destination.getItem(0), isStack(ItemStack.EMPTY));

        assertNoOverlap(source, destination);
    }

    @Test
    default void testMoveRotateWraps() {
        var source = new SimpleContainer(1);
        source.setItem(0, new ItemStack(Items.COBBLESTONE, 32));

        var destination = new SimpleContainer(9);
        destination.setItem(0, new ItemStack(Items.DIRT));
        for (var slot = 4; slot < 9; slot++) destination.setItem(slot, new ItemStack(Items.DIRT));

        var move = wrap(source).moveTo(wrap(destination).rotate(4), 64);
        assertEquals(32, move);

        assertThat("Source is empty", source.getItem(0), isStack(ItemStack.EMPTY));
        assertThat("Was inserted into slot", destination.getItem(1), isStack(Items.COBBLESTONE, 32));
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
