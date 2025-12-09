// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.detail;

import dan200.computercraft.api.detail.DetailProvider;
import dan200.computercraft.api.detail.DetailRegistry;
import net.minecraft.core.HolderLookup;

import java.util.*;

/**
 * Concrete implementation of {@link DetailRegistry}.
 *
 * @param <T> The type of object that this registry provides details for.
 */
public class DetailRegistryImpl<T> implements DetailRegistry<T> {
    private final Collection<DetailProvider<? super T>> providers = new ArrayList<>();
    private final DetailProvider<T> basic;

    public DetailRegistryImpl(DetailProvider<T> basic) {
        this.basic = basic;
        providers.add(basic);
    }

    @Override
    public synchronized void addProvider(DetailProvider<? super T> provider) {
        Objects.requireNonNull(provider, "provider cannot be null");
        if (!providers.contains(provider)) providers.add(provider);
    }

    @Override
    public Map<String, Object> getBasicDetails(HolderLookup.Provider registries, T object) {
        Objects.requireNonNull(object, "object cannot be null");

        Map<String, Object> map = new HashMap<>(4);
        basic.provideDetails(map, registries, object);
        return map;
    }

    @Override
    public Map<String, Object> getDetails(HolderLookup.Provider registries, T object) {
        Objects.requireNonNull(object, "object cannot be null");

        Map<String, Object> map = new HashMap<>();
        for (var provider : providers) provider.provideDetails(map, registries, object);
        return map;
    }
}
