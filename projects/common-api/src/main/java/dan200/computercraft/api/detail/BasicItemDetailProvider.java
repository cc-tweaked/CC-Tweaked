// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.detail;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An item detail provider for {@link ItemStack}s whose {@link Item} has a specific type.
 *
 * @param <T> The type the stack's item must have.
 */
public abstract class BasicItemDetailProvider<T> implements DetailProvider<ItemStack> {
    private final Class<T> itemType;
    private final @Nullable String namespace;

    /**
     * Create a new item detail provider. Details will be inserted into a new sub-map named as per {@code namespace}.
     *
     * @param itemType  The type the stack's item must have.
     * @param namespace The namespace to use for this provider.
     */
    public BasicItemDetailProvider(@Nullable String namespace, Class<T> itemType) {
        Objects.requireNonNull(itemType);
        this.itemType = itemType;
        this.namespace = namespace;
    }

    /**
     * Create a new item detail provider. Details will be inserted directly into the results.
     *
     * @param itemType The type the stack's item must have.
     */
    public BasicItemDetailProvider(Class<T> itemType) {
        this(null, itemType);
    }

    /**
     * Provide additional details for the given {@link Item} and {@link ItemStack}. This method is called by
     * {@code turtle.getItemDetail()}. New properties should be added to the given {@link Map}, {@code data}.
     * <p>
     * This method is always called on the server thread, so it is safe to interact with the world here, but you should
     * take care to avoid long blocking operations as this will stall the server and other computers.
     *
     * @param data  The full details to be returned for this item stack. New properties should be added to this map.
     * @param stack The item stack to provide details for.
     * @param item  The item to provide details for.
     */
    public abstract void provideDetails(Map<? super String, Object> data, ItemStack stack, T item);

    @Override
    public final void provideDetails(Map<? super String, Object> data, ItemStack stack) {
        var item = stack.getItem();
        if (!itemType.isInstance(item)) return;

        if (namespace == null) {
            provideDetails(data, stack, itemType.cast(item));
        } else {
            Map<? super String, Object> child = new HashMap<>();
            provideDetails(child, stack, itemType.cast(item));
            data.put(namespace, child);
        }
    }
}
