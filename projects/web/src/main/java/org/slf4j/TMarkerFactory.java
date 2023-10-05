// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package org.slf4j;

import java.io.Serial;
import java.util.Collections;
import java.util.Iterator;

/**
 * A replacement for SLF4J's {@link MarkerFactory}, which skips service loading and always uses a constant
 * {@link Marker}.
 */
public final class TMarkerFactory {
    private static final Marker INSTANCE = new MarkerImpl();

    private TMarkerFactory() {
    }

    public static Marker getMarker(String name) {
        return INSTANCE;
    }

    private static final class MarkerImpl implements Marker {
        @Serial
        private static final long serialVersionUID = 6353565105632304410L;

        @Override
        public String getName() {
            return "unnamed";
        }

        @Override
        public void add(Marker reference) {
        }

        @Override
        public boolean remove(Marker reference) {
            return false;
        }

        @Override
        @Deprecated
        public boolean hasChildren() {
            return false;
        }

        @Override
        public boolean hasReferences() {
            return false;
        }

        @Override
        public Iterator<Marker> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean contains(Marker other) {
            return false;
        }

        @Override
        public boolean contains(String name) {
            return false;
        }
    }
}
