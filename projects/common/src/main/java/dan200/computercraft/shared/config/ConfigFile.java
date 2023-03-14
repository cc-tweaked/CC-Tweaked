/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.config;

import com.google.common.base.Splitter;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A config file which the user can modify.
 */
public interface ConfigFile {
    String TRANSLATION_PREFIX = "gui.computercraft.config.";
    Splitter SPLITTER = Splitter.on('.');

    /**
     * An entry in the config file, either a {@link Value} or {@linkplain Group group of other entries}.
     */
    sealed interface Entry permits Group, Value {
        /**
         * Get the translation key of this config entry.
         *
         * @return This entry's translation key.
         */
        String translationKey();

        /**
         * Get the comment about this config entry.
         *
         * @return The comment for this config entry.
         */
        String comment();
    }

    /**
     * A configurable value.
     *
     * @param <T> The type of the stored value.
     */
    non-sealed interface Value<T> extends Entry, Supplier<T> {
    }

    /**
     * A group of config entries.
     */
    non-sealed interface Group extends Entry {
        /**
         * Get all entries in this group.
         *
         * @return All child entries.
         */
        Stream<Entry> children();
    }

    /**
     * Get a list of all config keys in this file.
     *
     * @return All config keys.
     */
    Stream<Entry> entries();

    @Nullable
    Entry getEntry(String path);

    /**
     * A builder which can be used to generate a config object.
     */
    abstract class Builder {
        protected final Deque<String> groupStack = new ArrayDeque<>();

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
        public abstract Builder comment(String comment);

        /**
         * Push a new config group.
         *
         * @param name The name of the group.
         */
        @OverridingMethodsMustInvokeSuper
        public void push(String name) {
            groupStack.addLast(name);
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

        public abstract <T> ConfigFile.Value<T> define(String path, T defaultValue);

        /**
         * A boolean-specific override of the above {@link #define(String, Object)} method.
         *
         * @param path         The path to the value we're defining.
         * @param defaultValue The default value.
         * @return The accessor for this config option.
         */
        public abstract ConfigFile.Value<Boolean> define(String path, boolean defaultValue);

        public abstract ConfigFile.Value<Integer> defineInRange(String path, int defaultValue, int min, int max);

        public abstract <T> ConfigFile.Value<List<? extends T>> defineList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator);

        public abstract <V extends Enum<V>> ConfigFile.Value<V> defineEnum(String path, V defaultValue);

        /**
         * Finalise this config file.
         *
         * @param onChange The function to run on change.
         * @return The built config file.
         */
        public abstract ConfigFile build(Runnable onChange);
    }
}
