// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

/**
 * A list which prepends a single value to another list.
 *
 * @param <T> The type of item in the list.
 */
public final class ConsList<T> extends AbstractList<T> {
    private final T head;
    private final List<T> tail;

    public ConsList(T head, List<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    @Override
    public T get(int index) {
        return index == 0 ? head : tail.get(index - 1);
    }

    @Override
    public int size() {
        return 1 + tail.size();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private @Nullable Iterator<T> tailIterator;

            @Override
            public boolean hasNext() {
                return tailIterator == null || tailIterator.hasNext();
            }

            @Override
            public T next() {
                if (tailIterator != null) return tailIterator.next();

                tailIterator = tail.iterator();
                return head;
            }
        };
    }
}
