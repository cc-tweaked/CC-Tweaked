// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Provides a delegate over inventories.
 * <p>
 * This may be used both on {@link BlockEntity}s to redirect the inventory to another tile,
 * and by other interfaces to have inventories which change their backing store.
 */
@FunctionalInterface
public interface InventoryDelegate extends Container {
    Container getInventory();

    @Override
    default int getContainerSize() {
        return getInventory().getContainerSize();
    }

    @Override
    default boolean isEmpty() {
        return getInventory().isEmpty();
    }

    @Override
    default ItemStack getItem(int slot) {
        return getInventory().getItem(slot);
    }

    @Override
    default ItemStack removeItem(int slot, int count) {
        return getInventory().removeItem(slot, count);
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return getInventory().removeItemNoUpdate(slot);
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        getInventory().setItem(slot, stack);
    }

    @Override
    default int getMaxStackSize() {
        return getInventory().getMaxStackSize();
    }

    @Override
    default void setChanged() {
        getInventory().setChanged();
    }

    @Override
    default boolean stillValid(Player player) {
        return getInventory().stillValid(player);
    }

    @Override
    default void startOpen(Player player) {
        getInventory().startOpen(player);
    }

    @Override
    default void stopOpen(Player player) {
        getInventory().stopOpen(player);
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return getInventory().canPlaceItem(slot, stack);
    }

    @Override
    default void clearContent() {
        getInventory().clearContent();
    }

    @Override
    default int countItem(Item stack) {
        return getInventory().countItem(stack);
    }

    @Override
    default boolean hasAnyOf(Set<Item> set) {
        return getInventory().hasAnyOf(set);
    }

    @Override
    default boolean hasAnyMatching(Predicate<ItemStack> predicate) {
        return getInventory().hasAnyMatching(predicate);
    }
}
