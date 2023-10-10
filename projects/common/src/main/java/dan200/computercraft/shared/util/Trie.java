// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A key-value map, where the key is a list of values.
 *
 * @param <K> The type of keys in this trie.
 * @param <V> The values in this map.
 */
public class Trie<K, V> {
    private @Nullable V current;
    private @Nullable Map<K, Trie<K, V>> children;

    public Trie<K, V> getChild(Iterable<K> key) {
        var self = this;
        for (var keyElement : key) {
            if (self.children == null) self.children = new HashMap<>(1);
            self = self.children.computeIfAbsent(keyElement, x -> new Trie<>());
        }

        return self;
    }

    public @Nullable V getValue(Iterable<K> key) {
        return getChild(key).current;
    }

    public void setValue(Iterable<K> key, V value) {
        getChild(key).current = value;
    }

    public Stream<V> stream() {
        return Stream.concat(
            current == null ? Stream.empty() : Stream.of(current),
            children == null ? Stream.empty() : children.values().stream().flatMap(Trie::stream)
        );
    }
}
