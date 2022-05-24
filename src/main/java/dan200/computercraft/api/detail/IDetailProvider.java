/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.detail;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * This interface is used to provide details about a block, fluid, or item.
 *
 * @param <T> The type of object that this provider can provide details for.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerDetailProvider(Class, IDetailProvider)
 */
@FunctionalInterface
public interface IDetailProvider<T>
{
    /**
     * Provide additional details for the given object. This method is called by functions such as
     * {@code turtle.getItemDetail()} and {@code turtle.inspect()}. New properties should be added to the given
     * {@link Map}, {@code data}.
     *
     * This method is always called on the server thread, so it is safe to interact with the world here, but you should
     * take care to avoid long blocking operations as this will stall the server and other computers.
     *
     * @param data   The full details to be returned. New properties should be added to this map.
     * @param object The object to provide details for.
     */
    void provideDetails( @Nonnull Map<? super String, Object> data, @Nonnull T object );
}
