// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.container;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * A basic implementation of {@link Container} which operates on a {@linkplain #getContents() list of stacks}.
 */
public interface BasicContainer extends Container {
    NonNullList<ItemStack> getContents();

    @Override
    default int getContainerSize() {
        return getContents().size();
    }

    @Override
    default boolean isEmpty() {
        for (var stack : getContents()) {
            if (!stack.isEmpty()) return false;
        }

        return true;
    }

    @Override
    default ItemStack getItem(int slot) {
        var contents = getContents();
        return slot >= 0 && slot < contents.size() ? contents.get(slot) : ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(getContents(), slot);
    }

    @Override
    default ItemStack removeItem(int slot, int count) {
        return ContainerHelper.removeItem(getContents(), slot, count);
    }

    @Override
    default void setItem(int slot, ItemStack itemStack) {
        getContents().set(slot, itemStack);
    }

    @Override
    default void clearContent() {
        getContents().clear();
    }
}
