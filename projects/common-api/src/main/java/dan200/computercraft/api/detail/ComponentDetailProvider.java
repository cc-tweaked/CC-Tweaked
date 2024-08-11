// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.detail;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An item detail provider for a specific {@linkplain DataComponentType data component} on {@link ItemStack}s or
 * other {@link DataComponentHolder}.
 *
 * @param <T> The type of the component's contents.
 */
public abstract class ComponentDetailProvider<T> implements DetailProvider<DataComponentHolder> {
    private final DataComponentType<T> component;
    private final @Nullable String namespace;

    /**
     * Create a new component detail provider. Details will be inserted into a new sub-map named as per {@code namespace}.
     *
     * @param component The data component to provide details for.
     * @param namespace The namespace to use for this provider.
     */
    public ComponentDetailProvider(@Nullable String namespace, DataComponentType<T> component) {
        Objects.requireNonNull(component);
        this.component = component;
        this.namespace = namespace;
    }

    /**
     * Create a new component detail provider. Details will be inserted directly into the results.
     *
     * @param component The data component to provide details for.
     */
    public ComponentDetailProvider(DataComponentType<T> component) {
        this(null, component);
    }

    /**
     * Provide additional details for the given data component. This method is called by {@code turtle.getItemDetail()}.
     * New properties should be added to the given {@link Map}, {@code data}.
     * <p>
     * This method is always called on the server thread, so it is safe to interact with the world here, but you should
     * take care to avoid long blocking operations as this will stall the server and other computers.
     *
     * @param data The full details to be returned for this item stack. New properties should be added to this map.
     * @param item The component to provide details for.
     */
    public abstract void provideComponentDetails(Map<? super String, Object> data, T item);

    @Override
    public final void provideDetails(Map<? super String, Object> data, DataComponentHolder holder) {
        var value = holder.get(component);
        if (value == null) return;

        if (namespace == null) {
            provideComponentDetails(data, value);
        } else {
            Map<? super String, Object> child = new HashMap<>();
            provideComponentDetails(child, value);
            data.put(namespace, child);
        }
    }
}
