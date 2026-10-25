// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeHandle;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;

/**
 * A concrete implementation of {@link UpgradeHandle}.
 *
 * @param <U> The type of upgrade.
 * @see AbstractUpgradeHolder
 */
final class UpgradeHandleImpl<U extends UpgradeBase> implements UpgradeHandle<U> {
    private final AbstractUpgradeHolder<U> holder;

    UpgradeHandleImpl(AbstractUpgradeHolder<U> holder) {
        this.holder = holder;
    }

    @Override
    public U getUpgrade() {
        holder.requireValid();
        return holder.getUpgrade();
    }

    @Override
    public ItemStack getItem() {
        holder.requireValid();
        return holder.getItem().copy();
    }

    @Override
    public void setItem(ItemStack newItem) {
        holder.requireValid();

        var upgrade = holder.getUpgradeHolder();
        if (newItem.getCount() != 1) {
            throw new IllegalArgumentException(
                "Error setting item for " + upgrade.key().location() + ": Item " + newItem + " must be of size 1."
            );
        }

        if (ItemStack.matches(holder.getItem(), newItem)) return;

        if (!upgrade.value().getCraftingIngredient().test(newItem) || !upgrade.value().isItemSuitable(newItem)) {
            throw new IllegalArgumentException(
                "Error setting item for " + upgrade.key().location() + ": Item " + newItem + " is not valid for this upgrade."
            );
        }

        holder.setItem(newItem);
        holder.setChanged();
    }

    @Override
    public DataComponentPatch getData() {
        holder.requireValid();
        return holder.getData();
    }

    @Override
    public void setData(DataComponentPatch data) {
        holder.requireValid();
        holder.setData(data);
        holder.setChanged();
    }

    @Override
    public void remove() {
        holder.requireValid();
        holder.remove();
    }
}
