// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.container;

import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A basic implementation of {@link Container} which operates on a {@linkplain #getItems() list of stacks}.
 */
public interface BasicContainer extends Container {
    List<ItemStack> getItems();

    @Override
    default int getContainerSize() {
        return getItems().size();
    }

    @Override
    default boolean isEmpty() {
        for (var stack : getItems()) {
            if (!stack.isEmpty()) return false;
        }

        return true;
    }

    @Override
    default ItemStack getItem(int slot) {
        var contents = getItems();
        return slot >= 0 && slot < contents.size() ? contents.get(slot) : ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(getItems(), slot);
    }

    @Override
    default ItemStack removeItem(int slot, int count) {
        return ContainerHelper.removeItem(getItems(), slot, count);
    }

    @Override
    default void setItem(int slot, ItemStack itemStack) {
        getItems().set(slot, itemStack);
    }

    @Override
    default void clearContent() {
        getItems().clear();
    }

    static void defaultSetItems(List<ItemStack> inventory, List<ItemStack> items) {
        var i = 0;
        for (; i < items.size(); i++) inventory.set(i, items.get(i));
        for (; i < inventory.size(); i++) inventory.set(i, ItemStack.EMPTY);
    }
}
