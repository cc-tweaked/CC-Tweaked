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
import dan200.computercraft.shared.util.Trie;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link ConfigFile} which sits directly on top of NightConfig.
 */
public class FabricConfigFile implements ConfigFile {
    private static final Logger LOG = LoggerFactory.getLogger(FabricConfigFile.class);

    private final ConfigSpec spec;
    private final Trie<String, Entry> entries;
    private final ConfigListener onChange;

    private @Nullable CommentedFileConfig config;

    public FabricConfigFile(ConfigSpec spec, Trie<String, Entry> entries, ConfigListener onChange) {
        this.spec = spec;
        this.entries = entries;
        this.onChange = onChange;
    }

    public synchronized void load(Path path) {
        closeConfig();

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
        return (Stream<ValueImpl<?>>) (Stream<?>) entries.stream().filter(ValueImpl.class::isInstance);
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
        entries.stream().forEach(x -> config.setComment(x.path, x.comment));
        var corrected = isNewFile ? spec.correct(config) : spec.correct(config, (action, entryPath, oldValue, newValue) -> {
            LOG.warn("Incorrect key {} was corrected from {} to {}", String.join(".", entryPath), oldValue, newValue);
        });

        // And then load the underlying entries.
        values().forEach(x -> x.load(config));
        onChange.onConfigChanged(config.getNioPath());

        return corrected > 0;
    }

    @Override
    public Stream<ConfigFile.Entry> entries() {
        return entries.stream().map(x -> (ConfigFile.Entry) x);
    }

    @Nullable
    @Override
    public ConfigFile.Entry getEntry(String path) {
        return (ConfigFile.Entry) entries.getValue(SPLITTER.split(path));
    }

    static class Builder extends ConfigFile.Builder {
        private final ConfigSpec spec = new ConfigSpec();
        private final Trie<String, Entry> entries = new Trie<>();

        private @Nullable String pendingComment;

        private String getFullPath(String path) {
            var key = new StringBuilder();
            for (var group : groupStack) key.append(group).append('.');
            key.append(path);
            return key.toString();
        }

        @Override
        public ConfigFile.Builder comment(String comment) {
            if (pendingComment != null) throw new IllegalStateException("Already have a comment");
            pendingComment = comment;
            return this;
        }

        private String takeComment() {
            var comment = pendingComment;
            if (comment == null) throw new IllegalStateException("No comment specified");
            pendingComment = null;
            return comment;
        }

        private String takeComment(String suffix) {
            var comment = pendingComment == null ? "" : pendingComment + "\n";
            pendingComment = null;
            return comment + suffix;
        }

        @Override
        public void push(String name) {
            var path = getFullPath(name);
            var splitPath = SPLITTER.split(path);
            entries.setValue(splitPath, new GroupImpl(path, takeComment()));

            super.push(name);
        }

        @Override
        public ConfigFile.Builder worldRestart() {
            return this;
        }

        private <T> Value<T> defineValue(String fullPath, String comment, T defaultValue, TriFunction<Config, String, T, T> getter) {
            var value = new ValueImpl<T>(fullPath, comment, defaultValue, getter);
            entries.setValue(SPLITTER.split(fullPath), value);
            return value;
        }

        @Override
        public <T> Value<T> define(String path, T defaultValue) {
            var fullPath = getFullPath(path);
            spec.define(fullPath, defaultValue);
            return defineValue(fullPath, takeComment(), defaultValue, Config::getOrElse);
        }

        @Override
        public Value<Boolean> define(String path, boolean defaultValue) {
            var fullPath = getFullPath(path);
            spec.define(fullPath, defaultValue, x -> x instanceof Boolean);
            return defineValue(fullPath, takeComment(), defaultValue, UnmodifiableConfig::getOrElse);
        }

        @Override
        public Value<Integer> defineInRange(String path, int defaultValue, int min, int max) {
            var fullPath = getFullPath(path);
            spec.defineInRange(fullPath, defaultValue, min, max);

            var suffix = max == Integer.MAX_VALUE ? "Range: > " + min : "Range: " + min + " ~ " + max;
            return defineValue(fullPath, takeComment(suffix), defaultValue, UnmodifiableConfig::getIntOrElse);
        }

        @Override
        public <T> Value<List<? extends T>> defineList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator) {
            var fullPath = getFullPath(path);
            spec.defineList(fullPath, defaultValue, elementValidator);
            return defineValue(fullPath, takeComment(), defaultValue, Config::getOrElse);
        }

        @Override
        public <V extends Enum<V>> Value<V> defineEnum(String path, V defaultValue) {
            var fullPath = getFullPath(path);
            spec.define(fullPath, defaultValue, o -> o != null && o != NullObject.NULL_OBJECT && EnumGetMethod.NAME_IGNORECASE.validate(o, defaultValue.getDeclaringClass()));

            var suffix = "Allowed Values: " + Arrays.stream(defaultValue.getDeclaringClass().getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
            return defineValue(fullPath, takeComment(suffix), defaultValue, (c, p, d) -> c.getEnumOrElse(p, d, EnumGetMethod.NAME_IGNORECASE));
        }

        @Override
        public ConfigFile build(ConfigListener onChange) {
            return new FabricConfigFile(spec, entries, onChange);
        }
    }

    private static class Entry {
        final String path;
        final String comment;

        Entry(String path, String comment) {
            this.path = path;
            this.comment = comment;
        }

        @SuppressWarnings("UnusedMethod")
        public final String translationKey() {
            return TRANSLATION_PREFIX + path;
        }

        @SuppressWarnings("UnusedMethod")
        public final String comment() {
            return comment;
        }
    }

    private static final class GroupImpl extends Entry implements Group {
        private GroupImpl(String path, String comment) {
            super(path, comment);
        }
    }

    private static final class ValueImpl<T> extends Entry implements Value<T> {
        private @Nullable T value;
        private final T defaultValue;
        private final TriFunction<Config, String, T, T> get;

        private ValueImpl(String path, String comment, T defaultValue, TriFunction<Config, String, T, T> get) {
            super(path, comment);
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
