// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.shared.config.ConfigFile;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A {@link ConfigFile} which wraps Forge's config implementation.
 */
public final class ForgeConfigFile extends ConfigFile {
    private final ModConfigSpec spec;

    private ForgeConfigFile(ModConfigSpec spec, Map<String, Entry> entries) {
        super(entries);
        this.spec = spec;
    }

    public ModConfigSpec spec() {
        return spec;
    }

    /**
     * Wraps {@link ModConfigSpec.Builder} into our own config builder abstraction.
     */
    static class Builder extends ConfigFile.Builder {
        private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        private void translation(String name) {
            builder.translation(getTranslation(name));
        }

        @Override
        public ConfigFile.Builder comment(String comment) {
            super.comment(comment);
            builder.comment(comment);
            return this;
        }

        @Override
        public void push(String name) {
            super.push(name);

            translation(name);
            builder.push(name);
        }

        @Override
        public void pop() {
            super.pop();
            builder.pop();
        }

        @Override
        public ConfigFile.Builder worldRestart() {
            builder.worldRestart();
            return this;
        }

        private <T> ConfigFile.Value<T> defineValue(String name, ModConfigSpec.ConfigValue<T> value) {
            var wrapped = new ValueImpl<>(getPath(name), takeComment(), value);
            groupStack.getLast().children().put(name, wrapped);
            return wrapped;
        }

        @Override
        public <T> ConfigFile.Value<T> define(String name, T defaultValue) {
            translation(name);
            return defineValue(name, builder.define(name, defaultValue));
        }

        @Override
        public ConfigFile.Value<Boolean> define(String name, boolean defaultValue) {
            translation(name);
            return defineValue(name, builder.define(name, defaultValue));
        }

        @Override
        public ConfigFile.Value<Integer> defineInRange(String name, int defaultValue, int min, int max) {
            translation(name);
            return defineValue(name, builder.defineInRange(name, defaultValue, min, max));
        }

        @Override
        public <T> ConfigFile.Value<List<? extends T>> defineList(String name, List<? extends T> defaultValue, Supplier<T> newValue, Predicate<Object> elementValidator) {
            translation(name);
            return defineValue(name, builder.defineList(name, defaultValue, newValue, elementValidator));
        }

        @Override
        public <V extends Enum<V>> ConfigFile.Value<V> defineEnum(String name, V defaultValue) {
            translation(name);
            return defineValue(name, builder.defineEnum(name, defaultValue));
        }

        @Override
        public ConfigFile build(ConfigListener onChange) {
            var children = groupStack.removeLast().children();
            if (!groupStack.isEmpty()) throw new IllegalStateException("Mismatched config push/pop");

            var spec = builder.build();
            return new ForgeConfigFile(spec, children);
        }
    }

    private static final class ValueImpl<T> extends Value<T> {
        private final ModConfigSpec.ConfigValue<T> value;

        private ValueImpl(String path, String comment, ModConfigSpec.ConfigValue<T> value) {
            super(path, comment);
            this.value = value;
        }

        @Override
        public T get() {
            return value.get();
        }
    }
}
