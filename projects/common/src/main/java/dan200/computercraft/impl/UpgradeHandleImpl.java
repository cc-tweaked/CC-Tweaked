// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.upgrades.MutableUpgradeHandle;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeHandle;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * A concrete implementation of {@link UpgradeHandle}.
 *
 * @param <U> The type of upgrade.
 * @see AbstractUpgradeHolder
 */
final class UpgradeHandleImpl<U extends UpgradeBase> implements MutableUpgradeHandle<U> {
    private final AbstractUpgradeHolder<U> holder;

    UpgradeHandleImpl(AbstractUpgradeHolder<U> holder) {
        this.holder = holder;
    }

    @Override
    public Holder.Reference<U> getUpgradeHolder() {
        holder.requireValid();
        return holder.getUpgradeHolder();
    }

    @Override
    public ItemStackTemplate getItem() {
        holder.requireValid();
        return holder.getItem();
    }

    @Override
    public void setItem(ItemStackTemplate newItem) {
        holder.requireValid();

        var upgrade = holder.getUpgradeHolder();
        if (newItem.count() != 1) {
            throw new IllegalArgumentException(
                "Error setting item for " + upgrade.key().identifier() + ": Item " + newItem + " must be of size 1."
            );
        }

        if (holder.getItem().equals(newItem)) return;

        var asStack = newItem.create();
        if (!upgrade.value().getCraftingIngredient().test(asStack) || !upgrade.value().isItemSuitable(asStack)) {
            throw new IllegalArgumentException(
                "Error setting item for " + upgrade.key().identifier() + ": Item " + newItem + " is not valid for this upgrade."
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
