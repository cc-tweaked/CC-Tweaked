// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * An upgrade (i.e. a {@link ITurtleUpgrade}) and its current upgrade data.
 * <p>
 * <strong>IMPORTANT:</strong> The {@link #data()} in an upgrade data is often a reference to the original upgrade data.
 * Be careful to take a {@linkplain #copy() defensive copy} if you plan to use the data in this upgrade.
 *
 * @param <T> The type of upgrade, either {@link ITurtleUpgrade} or {@link IPocketUpgrade}.
 */
public final class UpgradeData<T extends UpgradeBase> {
    private final T upgrade;
    private final CompoundTag data;
    private final @Nullable ItemStack stack;

    private UpgradeData(T upgrade, CompoundTag data, @Nullable ItemStack stack) {
        this.upgrade = upgrade;
        this.data = data;
        this.stack = stack;

        if (stack != null) {
            if (stack.getCount() != 1) {
                throw new IllegalArgumentException(
                    "Error constructing UpgradeData for " + upgrade.getUpgradeID() + ": Item " + stack + " must be of size 1."
                );
            }

            if (!upgrade.getCraftingIngredient().test(stack) || !upgrade.isItemSuitable(stack)) {
                throw new IllegalArgumentException(
                    "Error constructing UpgradeData for " + upgrade.getUpgradeID() + ": Item " + stack + " is not suitable for this upgrade."
                );
            }
        }
    }

    /**
     * Create a new {@link UpgradeData} with an item and upgrade data.
     *
     * @param upgrade The current upgrade.
     * @param data    The upgrade's data.
     * @deprecated Use {@link UpgradeData#of(UpgradeBase, ItemStack, CompoundTag)}.
     */
    @Deprecated(since = "1.121.0")
    public UpgradeData(T upgrade, @Unmodifiable CompoundTag data) {
        this(upgrade, data, null);
    }

    /**
     * A utility method to construct a new {@link UpgradeData} instance.
     *
     * @param upgrade An upgrade.
     * @param data    The upgrade's data. This is <strong>not</strong> copied, so should not be modified after use.
     * @param <T>     The type of upgrade.
     * @return The new {@link UpgradeData} instance.
     * @deprecated Use {@link #of(UpgradeBase, ItemStack, CompoundTag)}
     */
    @Deprecated(since = "1.121.0")
    public static <T extends UpgradeBase> UpgradeData<T> of(T upgrade, @Unmodifiable CompoundTag data) {
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
    public static <T extends UpgradeBase> UpgradeData<T> of(T upgrade, @Unmodifiable ItemStack stack, @Unmodifiable CompoundTag data) {
        return new UpgradeData<>(upgrade, data, stack.copy());
    }

    /**
     * Create an {@link UpgradeData} containing the default {@linkplain #data() data} for an upgrade.
     *
     * @param upgrade The upgrade instance.
     * @param <T>     The type of upgrade.
     * @return The default upgrade data.
     */
    public static <T extends UpgradeBase> UpgradeData<T> ofDefault(T upgrade) {
        return of(upgrade, upgrade.getCraftingItem(), upgrade.getUpgradeData(upgrade.getCraftingItem()));
    }

    /**
     * Take a copy of a (possibly {@code null}) {@link UpgradeData} instance.
     *
     * @param upgrade The copied upgrade data.
     * @param <T>     The type of upgrade.
     * @return The newly created upgrade data.
     */
    @Contract("!null -> !null; null -> null")
    public static <T extends UpgradeBase> @Nullable UpgradeData<T> copyOf(@Nullable UpgradeData<T> upgrade) {
        return upgrade == null ? null : upgrade.copy();
    }

    /**
     * Get the current upgrade.
     *
     * @return The current upgrade
     */
    public T upgrade() {
        return upgrade;
    }

    /**
     * Get the upgrade data.
     *
     * @return The upgrade data.
     */
    public @Unmodifiable CompoundTag data() {
        return data;
    }

    /**
     * Get the {@linkplain UpgradeBase#getUpgradeItem(CompoundTag) upgrade item} for this upgrade.
     * <p>
     * This returns a defensive copy of the item, to prevent accidental mutation of the stack.
     *
     * @return This upgrade's item.
     */
    public ItemStack getUpgradeItem() {
        return (stack == null ? upgrade.getUpgradeItem(data) : stack).copy();
    }

    /**
     * Take a copy of this {@link UpgradeData}. This returns a new instance with the same upgrade and a fresh copy of
     * the upgrade data.
     *
     * @return A copy of the current instance.
     */
    public UpgradeData<T> copy() {
        return new UpgradeData<>(upgrade(), data().copy(), stack);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        return obj instanceof UpgradeData<?> that && upgrade == that.upgrade && data.equals(that.data) && areStacksEqual(stack, that.stack);
    }

    private static boolean areStacksEqual(@Nullable ItemStack left, @Nullable ItemStack right) {
        if (left == null) return right == null;
        return right != null && ItemStack.isSameItemSameTags(left, right);
    }

    @Override
    public int hashCode() {
        var result = upgrade.hashCode();
        result = 31 * result + data.hashCode();
        result = 31 * result + hashStack(stack);
        return result;
    }

    private static int hashStack(@Nullable ItemStack stack) {
        return stack == null ? 0 : stack.getItem().hashCode() * 31 + Objects.hash(stack.getTag());
    }

    @Override
    public String toString() {
        return "UpgradeData[upgrade=" + upgrade + ", data=" + data + ", stack=" + stack + "]";
    }
}
