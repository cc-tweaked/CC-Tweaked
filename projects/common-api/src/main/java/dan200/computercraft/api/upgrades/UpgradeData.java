// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

/**
 * An upgrade (i.e. a {@link ITurtleUpgrade}) and its current upgrade data.
 *
 * @param <T> The type of upgrade, either {@link ITurtleUpgrade} or {@link IPocketUpgrade}.
 */
public final class UpgradeData<T extends UpgradeBase> implements DataComponentGetter {
    private final Holder.Reference<T> upgrade;
    private final DataComponentPatch data;
    private final @Nullable ItemStackTemplate stack;

    private UpgradeData(Holder.Reference<T> upgrade, DataComponentPatch data, @Nullable ItemStackTemplate stack) {
        if (!upgrade.isBound()) throw new IllegalArgumentException("Holder is not bound");

        this.upgrade = upgrade;
        this.data = data;
        this.stack = stack;

        if (stack != null) {
            if (stack.count() != 1) {
                throw new IllegalArgumentException(
                    "Error constructing UpgradeData for " + upgrade.key().identifier() + ": Item " + stack + " must be of size 1."
                );
            }

            if (!upgrade.value().getCraftingIngredient().acceptsItem(stack.item())) {
                throw new IllegalArgumentException(
                    "Error constructing UpgradeData for " + upgrade.key().identifier() + ": Item " + stack + " is not suitable for this upgrade."
                );
            }
        }
    }

    /**
     * Create a new {@link UpgradeData} with an item and upgrade data.
     *
     * @param upgrade The current upgrade.
     * @param data    The upgrade's data.
     * @deprecated Use {@link UpgradeData#of(Holder.Reference, ItemStackTemplate, DataComponentPatch)}.
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
     * @deprecated Use {@link #of(Holder.Reference, ItemStackTemplate, DataComponentPatch)}
     */
    @Deprecated(since = "1.121.0")
    public static <T extends UpgradeBase> UpgradeData<T> of(
        Holder.Reference<T> upgrade, DataComponentPatch data
    ) {
        return new UpgradeData<>(upgrade, data);
    }

    /**
     * A utility method to construct a new {@link UpgradeData} instance.
     *
     * @param <T>     The type of upgrade.
     * @param upgrade An upgrade.
     * @param stack   The upgrade's stack.
     * @param data    The upgrade's data.
     * @return The new {@link UpgradeData} instance.
     * @since 1.121.0
     */
    public static <T extends UpgradeBase> UpgradeData<T> of(Holder.Reference<T> upgrade, ItemStackTemplate stack, DataComponentPatch data) {
        return new UpgradeData<>(upgrade, data, stack);
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
     *
     * @return This upgrade's item.
     */
    public ItemStackTemplate getUpgradeItem() {
        return stack == null ? upgrade().getUpgradeItem(data) : stack;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        return obj instanceof UpgradeData<?> that
            && upgrade == that.upgrade && data.equals(that.data)
            && ((stack == null && that.stack == null) || getUpgradeItem().equals(that.getUpgradeItem()));
    }

    @Override
    public int hashCode() {
        return 31 * upgrade.hashCode() + data.hashCode();
    }

    @Override
    public String toString() {
        return "UpgradeData[upgrade=" + upgrade.key().identifier() + ", data=" + data + ", stack=" + stack + "]";
    }

    /**
     * Get a component from the upgrade's {@link #data()} .
     *
     * @param component The component get.
     * @param <U>       The type of the component's value.
     * @return The component, or {@code null} if not present.
     */
    @Override
    public <U> @Nullable U get(DataComponentType<? extends U> component) {
        return data().get(DataComponentMap.EMPTY, component);
    }
}
