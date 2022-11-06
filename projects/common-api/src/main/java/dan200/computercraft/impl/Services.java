/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Utilities for loading services.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public final class Services {
    private Services() {
    }

    /**
     * Load a service, asserting that only a single instance is registered.
     *
     * @param klass The class of the service to load.
     * @param <T>   The class of the service to load.
     * @return The constructed service instance.
     * @throws IllegalStateException When the service cannot be loaded.
     */
    public static <T> T load(Class<T> klass) {
        List<T> services = new ArrayList<>(1);
        for (var provider : ServiceLoader.load(klass)) services.add(provider);
        switch (services.size()) {
            case 1:
                return services.get(0);
            case 0:
                throw new IllegalStateException("Cannot find service for " + klass.getName());
            default:
                var serviceTypes = services.stream().map(x -> x.getClass().getName()).collect(Collectors.joining(", "));
                throw new IllegalStateException("Multiple services for " + klass.getName() + ": " + serviceTypes);
        }
    }

    /**
     * Attempt to load a service with {@link #load(Class)}.
     *
     * @param klass The class of the service to load.
     * @param <T>   The class of the service to load.
     * @return The result type, either containing the service or an exception.
     * @see ComputerCraftAPIService Intended usage of this class.
     */
    public static <T> LoadedService<T> tryLoad(Class<T> klass) {
        try {
            return new LoadedService<>(load(klass), null);
        } catch (Exception | LinkageError e) {
            return new LoadedService<>(null, e);
        }
    }

    /**
     * Raise an exception from trying to load a specific service.
     *
     * @param klass The class of the service we failed to load.
     * @param e     The original exception caused by loading this class.
     * @param <T>   The class of the service to load.
     * @return Never
     * @see #tryLoad(Class)
     * @see LoadedService#error()
     */
    @SuppressWarnings("DoNotCallSuggester")
    public static <T> T raise(Class<T> klass, @Nullable Throwable e) {
        // Throw a new exception so there's a useful stack trace there somewhere!
        throw new ServiceException("Failed to instantiate " + klass.getName(), e);
    }

    public static class LoadedService<T> {
        private final @Nullable T instance;
        private final @Nullable Throwable error;

        LoadedService(@Nullable T instance, @Nullable Throwable error) {
            this.instance = instance;
            this.error = error;
        }

        @Nullable
        public T instance() {
            return instance;
        }

        @Nullable
        public Throwable error() {
            return error;
        }
    }
}
