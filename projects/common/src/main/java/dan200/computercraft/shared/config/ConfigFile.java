// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.config;

import com.google.common.base.Splitter;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A config file which the user can modify.
 */
public abstract class ConfigFile {
    public static final String TRANSLATION_PREFIX = "gui.computercraft.config.";
    public static final Splitter SPLITTER = Splitter.on('.');

    /**
     * An entry in the config file, either a {@link Value} or {@linkplain Group group of other entries}.
     */
    public abstract static sealed class Entry permits Group, Value {
        protected final String path;
        private final String translationKey;
        private final String comment;

        protected Entry(String path, String comment) {
            this.path = path;
            this.translationKey = TRANSLATION_PREFIX + path;
            this.comment = comment;
        }

        public final String path() {
            return path;
        }

        /**
         * Get the translation key of this config entry.
         *
         * @return This entry's translation key.
         */
        public final String translationKey() {
            return translationKey;
        }

        /**
         * Get the comment about this config entry.
         *
         * @return The comment for this config entry.
         */
        public final String comment() {
            return comment;
        }

        abstract Stream<Entry> entries();
    }

    /**
     * A configurable value.
     *
     * @param <T> The type of the stored value.
     */
    public abstract static non-sealed class Value<T> extends Entry implements Supplier<T> {
        protected Value(String translationKey, String comment) {
            super(translationKey, comment);
        }

        @Override
        Stream<Entry> entries() {
            return Stream.of(this);
        }
    }

    /**
     * A group of config entries.
     */
    public static final class Group extends Entry {
        private final Map<String, Entry> children;

        public Group(String translationKey, String comment, Map<String, Entry> children) {
            super(translationKey, comment);
            this.children = children;
        }

        @Override
        Stream<Entry> entries() {
            return Stream.concat(Stream.of(this), children.values().stream().flatMap(Entry::entries));
        }
    }

    protected final Map<String, Entry> entries;

    protected ConfigFile(Map<String, Entry> entries) {
        this.entries = entries;
    }

    /**
     * Get a list of all config keys in this file.
     *
     * @return All config keys.
     */
    public final Stream<Entry> entries() {
        return entries.values().stream().flatMap(Entry::entries);
    }

    public final @Nullable Entry getEntry(String path) {
        var iterator = SPLITTER.split(path).iterator();

        var entry = entries.get(iterator.next());
        while (iterator.hasNext()) {
            if (!(entry instanceof Group group)) return null;
            entry = group.children.get(iterator.next());
        }

        return entry;
    }

    /**
     * A builder which can be used to generate a config object.
     */
    public abstract static class Builder {
        protected record RootGroup(String path, Map<String, Entry> children) {
            public RootGroup {
            }
        }

        protected final Deque<RootGroup> groupStack = new ArrayDeque<>();
        private @Nullable String pendingComment;

        protected Builder() {
            groupStack.addLast(new RootGroup("", new HashMap<>()));
        }

        protected final String getPath() {
            return groupStack.getLast().path();
        }

        protected final String getPath(String name) {
            var path = groupStack.getLast().path();
            return path.isEmpty() ? name : path + "." + name;
        }

        protected String getTranslation(String name) {
            var key = new StringBuilder(TRANSLATION_PREFIX);
            for (var group : groupStack) key.append(group).append('.');
            key.append(name);
            return key.toString();
        }

        /**
         * Add a comment to the next config object (either a {@linkplain #push(String) group} or a {@linkplain
         * #define(String, boolean) property}).
         *
         * @param comment The comment.
         * @return The current object, for chaining.
         */
        @OverridingMethodsMustInvokeSuper
        public ConfigFile.Builder comment(String comment) {
            if (pendingComment != null) throw new IllegalStateException("Already have a comment");
            pendingComment = comment;
            return this;
        }

        protected String takeComment() {
            var comment = pendingComment;
            if (comment == null) throw new IllegalStateException("No comment specified");
            pendingComment = null;
            return comment;
        }

        /**
         * Push a new config group.
         *
         * @param name The name of the group.
         */
        @OverridingMethodsMustInvokeSuper
        public void push(String name) {
            var path = getPath(name);
            Map<String, Entry> children = new HashMap<>();
            groupStack.getLast().children().put(name, new Group(path, takeComment(), children));
            groupStack.addLast(new RootGroup(path, children));
        }

        /**
         * Pop a config group.
         */
        @OverridingMethodsMustInvokeSuper
        public void pop() {
            groupStack.removeLast();
        }

        /**
         * Mark the next config property as requiring a world restart.
         *
         * @return The current object, for chaining.
         */
        public abstract Builder worldRestart();

        public abstract <T> ConfigFile.Value<T> define(String name, T defaultValue);

        /**
         * A boolean-specific override of the above {@link #define(String, Object)} method.
         *
         * @param name         The name of the value we're defining.
         * @param defaultValue The default value.
         * @return The accessor for this config option.
         */
        public abstract ConfigFile.Value<Boolean> define(String name, boolean defaultValue);

        public abstract ConfigFile.Value<Integer> defineInRange(String name, int defaultValue, int min, int max);

        public abstract <T> ConfigFile.Value<List<? extends T>> defineList(String name, List<? extends T> defaultValue, Supplier<T> newValue, Predicate<Object> elementValidator);

        public abstract <V extends Enum<V>> ConfigFile.Value<V> defineEnum(String name, V defaultValue);

        /**
         * Finalise this config file.
         *
         * @param onChange The function to run on change.
         * @return The built config file.
         */
        public abstract ConfigFile build(ConfigListener onChange);
    }

    @FunctionalInterface
    public interface ConfigListener {
        /**
         * The function called then a config file is changed.
         *
         * @param path The path to the config file. This will be {@code null} when the config file does not exist on
         *             disk, such as when synced from a server to the client.
         * @see Builder#build(ConfigListener)
         */
        void onConfigChanged(@Nullable Path path);
    }
}
