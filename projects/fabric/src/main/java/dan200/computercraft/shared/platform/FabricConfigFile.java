// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.electronwill.nightconfig.core.*;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import dan200.computercraft.shared.config.ConfigFile;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link ConfigFile} which sits directly on top of NightConfig.
 */
public final class FabricConfigFile extends ConfigFile {
    private static final Logger LOG = LoggerFactory.getLogger(FabricConfigFile.class);

    private final ConfigSpec spec;
    private final ConfigListener onChange;

    private @Nullable CommentedFileConfig config;

    private FabricConfigFile(ConfigSpec spec, Map<String, Entry> entries, ConfigListener onChange) {
        super(entries);
        this.spec = spec;
        this.onChange = onChange;
    }

    /**
     * Load the config from one or more possible paths.
     * <p>
     * Config files will be loaded from the first path which exists. If none exists, a file at the last location will
     * be chosen.
     *
     * @param paths The list of paths to load from.
     */
    public synchronized void load(Path... paths) {
        if (paths.length == 0) throw new IllegalArgumentException("Must pass at least one path");

        closeConfig();

        var path = Arrays.stream(paths).filter(Files::exists).findFirst().orElseGet(() -> paths[paths.length - 1]);
        var config = this.config = CommentedFileConfig.builder(path).sync()
            .onFileNotFound(FileNotFoundAction.READ_NOTHING)
            .writingMode(WritingMode.REPLACE)
            .build();

        try {
            Files.createDirectories(path.getParent());
            FileWatcher.defaultInstance().addWatch(config.getNioPath(), this::loadConfig);
        } catch (IOException e) {
            LOG.error("Failed to load config from {}.", path, e);
        }

        if (loadConfig()) config.save();
    }

    @SuppressWarnings("unchecked")
    private Stream<ValueImpl<?>> values() {
        return (Stream<ValueImpl<?>>) (Stream<?>) entries().filter(ValueImpl.class::isInstance);
    }

    public synchronized void unload() {
        closeConfig();

        values().forEach(ValueImpl::unload);
    }

    @GuardedBy("this")
    private void closeConfig() {
        if (config == null) return;

        config.close();
        FileWatcher.defaultInstance().removeWatch(config.getNioPath());
        config = null;
    }

    private synchronized boolean loadConfig() {
        var config = this.config;
        if (config == null) return false;

        LOG.info("Loading config from {}", config.getNioPath());

        config.load();

        // Ensure the config file matches the spec
        var isNewFile = config.isEmpty();
        entries().forEach(x -> config.setComment(x.path(), x instanceof ValueImpl<?> v ? v.fullComment : x.comment()));
        var corrected = isNewFile ? spec.correct(config) : spec.correct(config, (action, entryPath, oldValue, newValue) -> {
            LOG.warn("Incorrect key {} was corrected from {} to {}", String.join(".", entryPath), oldValue, newValue);
        });

        // And then load the underlying entries.
        values().forEach(x -> x.load(config));
        onChange.onConfigChanged(config.getNioPath());

        return corrected > 0;
    }

    static class Builder extends ConfigFile.Builder {
        private final ConfigSpec spec = new ConfigSpec();

        @Override
        public ConfigFile.Builder worldRestart() {
            return this;
        }

        private <T> Value<T> defineValue(String name, String comment, @Nullable String suffix, T defaultValue, TriFunction<Config, String, T, T> getter) {
            var fullComment = suffix == null ? comment : comment + "\n" + suffix;
            var value = new ValueImpl<T>(getPath(name), comment, fullComment, defaultValue, getter);
            groupStack.getLast().children().put(name, value);
            return value;
        }

        @Override
        public <T> Value<T> define(String name, T defaultValue) {
            var path = getPath(name);
            spec.define(path, defaultValue);
            return defineValue(name, takeComment(), null, defaultValue, Config::getOrElse);
        }

        @Override
        public Value<Boolean> define(String name, boolean defaultValue) {
            var path = getPath(name);
            spec.define(path, defaultValue, x -> x instanceof Boolean);
            return defineValue(name, takeComment(), null, defaultValue, UnmodifiableConfig::getOrElse);
        }

        @Override
        public Value<Integer> defineInRange(String name, int defaultValue, int min, int max) {
            var path = getPath(name);
            spec.defineInRange(path, defaultValue, min, max);

            var suffix = max == Integer.MAX_VALUE ? "Range: > " + min : "Range: " + min + " ~ " + max;
            return defineValue(name, takeComment(), suffix, defaultValue, UnmodifiableConfig::getIntOrElse);
        }

        @Override
        public <T> Value<List<? extends T>> defineList(String name, List<? extends T> defaultValue, Supplier<T> newValue, Predicate<Object> elementValidator) {
            var path = getPath(name);
            spec.defineList(path, defaultValue, elementValidator);
            return defineValue(name, takeComment(), null, defaultValue, Config::getOrElse);
        }

        @Override
        public <V extends Enum<V>> Value<V> defineEnum(String name, V defaultValue) {
            var path = getPath(name);
            spec.define(path, defaultValue, o -> o != null && o != NullObject.NULL_OBJECT && EnumGetMethod.NAME_IGNORECASE.validate(o, defaultValue.getDeclaringClass()));

            var suffix = "Allowed Values: " + Arrays.stream(defaultValue.getDeclaringClass().getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
            return defineValue(name, takeComment(), suffix, defaultValue, (c, p, d) -> c.getEnumOrElse(p, d, EnumGetMethod.NAME_IGNORECASE));
        }

        @Override
        public ConfigFile build(ConfigListener onChange) {
            var children = groupStack.removeLast().children();
            if (!groupStack.isEmpty()) throw new IllegalStateException("Mismatched config push/pop");
            return new FabricConfigFile(spec, children, onChange);
        }
    }

    private static final class ValueImpl<T> extends Value<T> {
        private @Nullable T value;
        private final T defaultValue;
        private final TriFunction<Config, String, T, T> get;
        private final String fullComment;

        private ValueImpl(String path, String comment, String fullComment, T defaultValue, TriFunction<Config, String, T, T> get) {
            super(path, comment);
            this.fullComment = fullComment;
            this.defaultValue = defaultValue;
            this.get = get;
        }

        void unload() {
            value = null;
        }

        void load(Config config) {
            value = get.apply(config, path, defaultValue);
        }

        @Override
        public T get() {
            var value = this.value;
            if (value == null) throw new IllegalStateException("Config value " + path + " is not available");
            return value;
        }
    }
}
