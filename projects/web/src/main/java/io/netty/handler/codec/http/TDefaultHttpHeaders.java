// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package io.netty.handler.codec.http;

import java.util.*;

/**
 * A replacement for {@link DefaultHttpHeaders}.
 * <p>
 * The default implementation does additional conversion for dates, which ends up pulling in a lot of code we can't
 * compile.
 */
public class TDefaultHttpHeaders extends HttpHeaders {
    private final Map<String, String> map = new HashMap<>();

    @Override
    public String get(String name) {
        return map.get(normalise(name));
    }

    @Override
    public List<String> getAll(String name) {
        var value = get(name);
        return value == null ? List.of() : List.of(value);
    }

    @Override
    public List<Map.Entry<String, String>> entries() {
        return List.copyOf(map.entrySet());
    }

    @Override
    public boolean contains(String name) {
        return get(name) != null;
    }

    @Override
    @Deprecated
    public Iterator<Map.Entry<String, String>> iterator() {
        return map.entrySet().iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Map.Entry<CharSequence, CharSequence>> iteratorCharSequence() {
        return (Iterator<Map.Entry<CharSequence, CharSequence>>) (Iterator<?>) map.entrySet().iterator();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Set<String> names() {
        return map.keySet();
    }

    @Override
    public HttpHeaders add(String name, Object value) {
        return set(name, value);
    }

    @Override
    public HttpHeaders set(String name, Object value) {
        map.put(normalise(name), (String) value);
        return this;
    }

    @Override
    public HttpHeaders remove(String name) {
        map.remove(normalise(name));
        return this;
    }

    @Override
    public HttpHeaders clear() {
        map.clear();
        return this;
    }

    //region Uncalled/unsupported methods
    @Override
    public Integer getInt(CharSequence name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(CharSequence name, int defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Short getShort(CharSequence name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShort(CharSequence name, short defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getTimeMillis(CharSequence name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTimeMillis(CharSequence name, long defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders add(String name, Iterable<?> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders addInt(CharSequence name, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders addShort(CharSequence name, short value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders set(String name, Iterable<?> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders setInt(CharSequence name, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders setShort(CharSequence name, short value) {
        throw new UnsupportedOperationException();
    }
    //endregion

    private static String normalise(String string) {
        return string.toLowerCase(Locale.ROOT);
    }
}
