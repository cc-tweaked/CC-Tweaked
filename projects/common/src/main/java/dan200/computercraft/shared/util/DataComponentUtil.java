// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.core.component.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

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

    public static <T> ItemStack createStack(ItemLike item, DataComponentType<T> type, @Nullable T value) {
        return set(new ItemStack(item), type, value);
    }

    public static <T> ItemStackTemplate createTemplate(Item item, DataComponentType<T> type, T value) {
        return new ItemStackTemplate(item, DataComponentPatch.builder().set(type, value).build());
    }

    /**
     * Create a stack dyed with a particular colour, but with the colour hidden from the tooltip.
     *
     * @param item   The item to create the stack from.
     * @param colour The stack's colour.
     * @return The newly created stack.
     */
    public static ItemStack createDyedStack(ItemLike item, int colour) {
        return setDyeColour(new ItemStack(item), colour);
    }

    public static ItemStack setDyeColour(ItemStack stack, int colour) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(colour));
        return stack;
    }

    /**
     * Check a component is present in a {@link DataComponentPatch} and matches the supplied predicate.
     *
     * @param patch     The current component patch.
     * @param component The component type.
     * @param check     The predicate to check against.
     * @param <T>       The type of component.
     * @return Whether the component is present in this patch, and matches the supplied predicate.
     */
    public static <T> boolean isPresent(DataComponentPatch patch, DataComponentType<T> component, Predicate<T> check) {
        var value = patch.get(DataComponentMap.EMPTY, component);
        return value != null && check.test(value);
    }
}
