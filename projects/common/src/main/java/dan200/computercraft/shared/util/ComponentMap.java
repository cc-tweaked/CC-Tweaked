// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import dan200.computercraft.api.component.ComputerComponent;
import dan200.computercraft.core.metrics.MetricsObserver;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * An immutable map of components.
 */
public final class ComponentMap {
    public static final ComputerComponent<MetricsObserver> METRICS = ComputerComponent.create("computercraft", "metrics");

    private static final ComponentMap EMPTY = new ComponentMap(Map.of());

    private final Map<ComputerComponent<?>, Object> components;

    private ComponentMap(Map<ComputerComponent<?>, Object> components) {
        this.components = components;
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(ComputerComponent<T> component) {
        return (T) components.get(component);
    }

    public static ComponentMap empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<ComputerComponent<?>, Object> components = new HashMap<>();

        private Builder() {
        }

        public <T> Builder add(ComputerComponent<T> component, T value) {
            addImpl(component, value);
            return this;
        }

        public Builder add(ComponentMap components) {
            for (var component : components.components.entrySet()) addImpl(component.getKey(), component.getValue());
            return this;
        }

        private void addImpl(ComputerComponent<?> component, Object value) {
            if (components.containsKey(component)) throw new IllegalArgumentException(component + " is already set");
            components.put(component, value);
        }

        public ComponentMap build() {
            return new ComponentMap(Map.copyOf(components));
        }
    }
}
