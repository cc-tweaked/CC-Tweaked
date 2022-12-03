/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.detail;

import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * A registry which provides computer-visible detail about in-game objects such as blocks, items or fluids.
 * <p>
 * These are used by computer methods such as {@code turtle.getItemDetail()} or {@code turtle.inspect()}.
 * <p>
 * Specific instances of this registry are available from {@link VanillaDetailRegistries} and loader-specific versions
 * also in this package.
 *
 * @param <T> The type of object that this registry provides details for.
 */
@ApiStatus.NonExtendable
public interface DetailRegistry<T> {
    /**
     * Registers a detail provider.
     *
     * @param provider The detail provider to register.
     * @see DetailProvider
     */
    void addProvider(DetailProvider<T> provider);

    /**
     * Compute basic details about an object. This is cheaper than computing all details operation, and so is suitable
     * for when you need to compute the details for a large number of values.
     *
     * @param object The object to get details for.
     * @return The basic details.
     */
    Map<String, Object> getBasicDetails(T object);

    /**
     * Compute all details about an object, using {@link #getBasicDetails(Object)} and any registered providers.
     *
     * @param object The object to get details for.
     * @return The computed details.
     */
    Map<String, Object> getDetails(T object);
}
