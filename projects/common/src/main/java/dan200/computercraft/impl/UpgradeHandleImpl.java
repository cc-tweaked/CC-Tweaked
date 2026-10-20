package dan200.computercraft.impl;

import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeHandle;
import net.minecraft.nbt.CompoundTag;
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
        if (newItem.getCount() != 1) throw new IllegalArgumentException("Item must have a count of 1");

        if (ItemStack.isSameItemSameTags(holder.getItem(), newItem)) return;

        if (!holder.getUpgrade().isItemSuitable(newItem)) {
            throw new IllegalArgumentException("Item is not valid for upgrade");
        }

        holder.setItem(newItem);
        holder.setChanged();
    }

    @Override
    public CompoundTag getData() {
        holder.requireValid();
        return holder.getData();
    }

    @Override
    public void setDataChanged() {
        holder.requireValid();
        holder.setChanged();
    }

    @Override
    public void remove() {
        holder.requireValid();
        holder.remove();
    }
}
