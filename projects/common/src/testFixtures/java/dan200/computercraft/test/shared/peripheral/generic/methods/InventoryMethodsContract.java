// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared.peripheral.generic.methods;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.shared.peripheral.generic.methods.AbstractInventoryMethods;
import dan200.computercraft.test.shared.WithMinecraft;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test Interface defining the behaviour of a {@link AbstractInventoryMethods} implementation.
 *
 * @param <T> The type for inventories.
 */
@WithMinecraft
public interface InventoryMethodsContract<T> {
    /**
     * Create our {@link AbstractInventoryMethods} implementation.
     *
     * @return The inventory methods.
     */
    AbstractInventoryMethods<T> create();

    /**
     * Wrap a basic container into a mod-loader-specific inventory.
     *
     * @param container The container to wrap.
     * @return The wrapped inventory.
     */
    T wrap(Container container);

    @Test
    default void testGetItemLimit() throws LuaException {
        var container = new SimpleContainer(3);
        container.setItem(0, new ItemStack(Items.DIRT, 1));
        container.setItem(1, new ItemStack(Items.WATER_BUCKET, 1));

        assertEquals(64, create().getItemLimit(wrap(container), 1), "Dirt stacks to 64");
        assertEquals(1, create().getItemLimit(wrap(container), 2), "Buckets stack to 1");
        assertEquals(64, create().getItemLimit(wrap(container), 3), "Empty slots stack to 64 by default");

        var err = assertThrows(LuaException.class, () -> create().getItemLimit(wrap(container), 0));
        assertEquals("Slot out of range (between 1 and 3)", err.getMessage());
    }
}
