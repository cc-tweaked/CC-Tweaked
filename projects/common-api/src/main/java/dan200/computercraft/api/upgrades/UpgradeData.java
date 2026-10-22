// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

/**
 * An upgrade (i.e. a {@link ITurtleUpgrade}) and its current upgrade data.
 *
 * @param <T> The type of upgrade, either {@link ITurtleUpgrade} or {@link IPocketUpgrade}.
 */
public final class UpgradeData<T extends UpgradeBase> {
    private final Holder.Reference<T> upgrade;
    private final DataComponentPatch data;
    private final @Nullable ItemStack stack;

    private UpgradeData(Holder.Reference<T> upgrade, DataComponentPatch data, @Nullable ItemStack stack) {
        if (!upgrade.isBound()) throw new IllegalArgumentException("Holder is not bound");

        this.upgrade = upgrade;
        this.data = data;
        this.stack = stack;

        if (stack != null) {
            if (stack.getCount() != 1) {
                throw new IllegalArgumentException(
                    "Error constructing UpgradeData for " + upgrade.key().location() + ": Item " + stack + " must be of size 1."
                );
            }

            if (!upgrade.value().getCraftingIngredient().test(stack) || !upgrade.value().isItemSuitable(stack)) {
                throw new IllegalArgumentException(
                    "Error constructing UpgradeData for " + upgrade.key().location() + ": Item " + stack + " is not suitable for this upgrade."
                );
            }
        }
    }

    /**
     * Create a new {@link UpgradeData} with an item and upgrade data.
     *
     * @param upgrade The current upgrade.
     * @param data    The upgrade's data.
     * @deprecated Use {@link UpgradeData#of(Holder.Reference, ItemStack, DataComponentPatch)}.
     */
    @Deprecated(since = "1.121.0")
    public UpgradeData(Holder.Reference<T> upgrade, DataComponentPatch data) {
        this(upgrade, data, null);
    }

    /**
     * A utility method to construct a new {@link UpgradeData} instance.
     *
     * @param upgrade The upgrade.
     * @param data    The upgrade's data. This is <strong>not</strong> copied, so should not be modified after use.
     * @param <T>     The type of upgrade.
     * @return The new {@link UpgradeData} instance.
     * @deprecated Use {@link #of(Holder.Reference, ItemStack, DataComponentPatch)}
     */
    @Deprecated(since = "1.121.0")
    public static <T extends UpgradeBase> UpgradeData<T> of(Holder.Reference<T> upgrade, DataComponentPatch data) {
        return new UpgradeData<>(upgrade, data);
    }

    /**
     * A utility method to construct a new {@link UpgradeData} instance.
     *
     * @param <T>     The type of upgrade.
     * @param upgrade An upgrade.
     * @param stack   The upgrade's stack. This is <strong>not</strong> copied, so should not be modified after use.
     * @param data    The upgrade's data. This is <strong>not</strong> copied, so should not be modified after use.
     * @return The new {@link UpgradeData} instance.
     * @since 1.121.0
     */
    public static <T extends UpgradeBase> UpgradeData<T> of(Holder.Reference<T> upgrade, @Unmodifiable ItemStack stack, @Unmodifiable DataComponentPatch data) {
        return new UpgradeData<>(upgrade, data, stack.copy());
    }

    /**
     * Create an {@link UpgradeData} containing the default {@linkplain #data() data} for an upgrade.
     *
     * @param holder The upgrade instance.
     * @param <T>    The type of upgrade.
     * @return The default upgrade data.
     */
    public static <T extends UpgradeBase> UpgradeData<T> ofDefault(Holder.Reference<T> holder) {
        var upgrade = holder.value();
        return of(holder, upgrade.getCraftingItem(), upgrade.getUpgradeData(upgrade.getCraftingItem()));
    }

    /**
     * Get the current upgrade.
     *
     * @return The current upgrade
     */
    public Holder.Reference<T> holder() {
        return upgrade;
    }

    /**
     * Get the current upgrade.
     *
     * @return The current upgrade.
     */
    public T upgrade() {
        return holder().value();
    }

    /**
     * Get the upgrade data.
     *
     * @return The upgrade data.
     */
    public DataComponentPatch data() {
        return data;
    }

    /**
     * Get the {@linkplain UpgradeBase#getUpgradeItem(DataComponentPatch) upgrade item} for this upgrade.
     * <p>
     * This returns a defensive copy of the item, to prevent accidental mutation of the stack.
     *
     * @return This upgrade's item.
     */
    public ItemStack getUpgradeItem() {
        return getStack().copy();
    }

    private ItemStack getStack() {
        return stack == null ? upgrade().getUpgradeItem(data) : stack;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        return obj instanceof UpgradeData<?> that
            && upgrade == that.upgrade && data.equals(that.data)
            && ((stack == null && that.stack == null) || ItemStack.matches(getStack(), that.getStack()));
    }

    @Override
    public int hashCode() {
        return 31 * upgrade.hashCode() + data.hashCode();
    }

    @Override
    public String toString() {
        return "UpgradeData[upgrade=" + upgrade.key().location() + ", data=" + data + ", stack=" + (stack == null ? "null" : stack.getItem().toString() + stack.getComponentsPatch()) + "]";
    }
}
