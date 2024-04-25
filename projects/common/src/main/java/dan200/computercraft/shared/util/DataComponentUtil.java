// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;

/**
 * Utilities for working with {@linkplain DataComponentType data components}.
 */
public class DataComponentUtil {
    public static @Nullable String getCustomName(DataComponentHolder holder) {
        return getCustomName(holder.get(DataComponents.CUSTOM_NAME));
    }

    @Contract("null -> null; !null -> !null")
    public static @Nullable String getCustomName(@Nullable Component name) {
        return name != null ? name.getString() : null;
    }

    public static void setCustomName(ItemStack stack, @Nullable String label) {
        stack.set(DataComponents.CUSTOM_NAME, label == null ? null : Component.literal(label));
    }

    private static <T> ItemStack set(ItemStack stack, DataComponentType<T> type, @Nullable T value) {
        stack.set(type, value);
        return stack;
    }

    public static <T> ItemStack createResult(ItemStack stack, DataComponentType<T> type, @Nullable T value) {
        return set(stack.copyWithCount(1), type, value);
    }

    public static <T> ItemStack createStack(ItemLike item, DataComponentType<T> type, @Nullable T value) {
        return set(new ItemStack(item), type, value);
    }
}
