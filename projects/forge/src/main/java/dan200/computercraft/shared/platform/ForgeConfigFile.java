// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.shared.config.ConfigFile;
import dan200.computercraft.shared.util.Trie;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A {@link ConfigFile} which wraps Forge's config implementation.
 */
public final class ForgeConfigFile implements ConfigFile {
    private final ForgeConfigSpec spec;
    private final Trie<String, ConfigFile.Entry> entries;

    public ForgeConfigFile(ForgeConfigSpec spec, Trie<String, Entry> entries) {
        this.spec = spec;
        this.entries = entries;
    }

    public ForgeConfigSpec spec() {
        return spec;
    }

    @Override
    public Stream<Entry> entries() {
        return entries.stream();
    }

    @Nullable
    @Override
    public Entry getEntry(String path) {
        return entries.getValue(SPLITTER.split(path));
    }

    /**
     * Wraps {@link ForgeConfigSpec.Builder} into our own config builder abstraction.
     */
    static class Builder extends ConfigFile.Builder {
        private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        private final Trie<String, ConfigFile.Entry> entries = new Trie<>();

        private void translation(String name) {
            builder.translation(getTranslation(name));
        }

        @Override
        public ConfigFile.Builder comment(String comment) {
            builder.comment(comment);
            return this;
        }

        @Override
        public void push(String name) {
            translation(name);
            builder.push(name);
            super.push(name);
        }

        @Override
        public void pop() {
            var path = new ArrayList<>(groupStack);
            entries.setValue(path, new GroupImpl(path));

            builder.pop();
            super.pop();
        }

        @Override
        public ConfigFile.Builder worldRestart() {
            builder.worldRestart();
            return this;
        }

        private <T> ConfigFile.Value<T> defineValue(ForgeConfigSpec.ConfigValue<T> value) {
            var wrapped = new ValueImpl<>(value);
            entries.setValue(value.getPath(), wrapped);
            return wrapped;
        }

        @Override
        public <T> ConfigFile.Value<T> define(String path, T defaultValue) {
            translation(path);
            return defineValue(builder.define(path, defaultValue));
        }

        @Override
        public ConfigFile.Value<Boolean> define(String path, boolean defaultValue) {
            translation(path);
            return defineValue(builder.define(path, defaultValue));
        }

        @Override
        public ConfigFile.Value<Integer> defineInRange(String path, int defaultValue, int min, int max) {
            translation(path);
            return defineValue(builder.defineInRange(path, defaultValue, min, max));
        }

        @Override
        public <T> ConfigFile.Value<List<? extends T>> defineList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator) {
            translation(path);
            return defineValue(builder.defineList(path, defaultValue, elementValidator));
        }

        @Override
        public <V extends Enum<V>> ConfigFile.Value<V> defineEnum(String path, V defaultValue) {
            translation(path);
            return defineValue(builder.defineEnum(path, defaultValue));
        }

        @Override
        public ConfigFile build(ConfigListener onChange) {
            var spec = builder.build();
            entries.stream().forEach(x -> {
                if (x instanceof ValueImpl<?> value) value.owner = spec;
                if (x instanceof GroupImpl value) value.owner = spec;
            });
            return new ForgeConfigFile(spec, entries);
        }
    }

    private static final class GroupImpl implements ConfigFile.Group {
        private final List<String> path;
        private @Nullable ForgeConfigSpec owner;

        private GroupImpl(List<String> path) {
            this.path = path;
        }

        @Override
        public String translationKey() {
            if (owner == null) throw new IllegalStateException("Config has not been built yet");
            return owner.getLevelTranslationKey(path);
        }

        @Override
        public String comment() {
            if (owner == null) throw new IllegalStateException("Config has not been built yet");
            return owner.getLevelComment(path);
        }
    }

    private static final class ValueImpl<T> implements ConfigFile.Value<T> {
        private final ForgeConfigSpec.ConfigValue<T> value;
        private @Nullable ForgeConfigSpec owner;

        private ValueImpl(ForgeConfigSpec.ConfigValue<T> value) {
            this.value = value;
        }

        private ForgeConfigSpec.ValueSpec spec() {
            if (owner == null) throw new IllegalStateException("Config has not been built yet");
            return owner.getSpec().get(value.getPath());
        }

        @Override
        public T get() {
            return value.get();
        }

        @Override
        public String translationKey() {
            return spec().getTranslationKey();
        }

        @Override
        public String comment() {
            return spec().getComment();
        }
    }
}
