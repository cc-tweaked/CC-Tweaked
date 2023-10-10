// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.detail;

import java.util.Map;

/**
 * Provide details about a block, fluid, or item.
 * <p>
 * When implementing this interface, be careful to only expose information the player can see through normal gameplay.
 * Computers shouldn't break progression or mechanics of other mods.
 *
 * @param <T> The type of object that this provider can provide details for.
 * @see DetailRegistry
 */
@FunctionalInterface
public interface DetailProvider<T> {
    /**
     * Provide additional details for the given object. This method is called by functions such as
     * {@code turtle.getItemDetail()} and {@code turtle.inspect()}. New properties should be added to the given
     * {@link Map}, {@code data}.
     * <p>
     * This method is always called on the server thread, so it is safe to interact with the world here, but you should
     * take care to avoid long blocking operations as this will stall the server and other computers.
     *
     * @param data   The full details to be returned. New properties should be added to this map.
     * @param object The object to provide details for.
     */
    void provideDetails(Map<? super String, Object> data, T object);
}
