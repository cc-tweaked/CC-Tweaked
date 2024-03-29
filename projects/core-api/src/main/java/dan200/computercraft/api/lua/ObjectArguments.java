// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of {@link IArguments} which wraps an array of {@link Object}.
 */
public final class ObjectArguments implements IArguments {
    private static final IArguments EMPTY = new ObjectArguments();

    private final List<Object> args;

    @Deprecated
    @SuppressWarnings("unused")
    public ObjectArguments(IArguments arguments) {
        throw new IllegalStateException();
    }

    public ObjectArguments(Object... args) {
        this.args = Arrays.asList(args);
    }

    public ObjectArguments(List<Object> args) {
        this.args = Objects.requireNonNull(args);
    }

    @Override
    public int count() {
        return args.size();
    }

    @Override
    public IArguments drop(int count) {
        if (count < 0) throw new IllegalStateException("count cannot be negative");
        if (count == 0) return this;
        if (count >= args.size()) return EMPTY;

        return new ObjectArguments(args.subList(count, args.size()));
    }

    @Nullable
    @Override
    public Object get(int index) {
        return index >= args.size() ? null : args.get(index);
    }

    @Override
    public String getType(int index) {
        return LuaValues.getType(get(index));
    }

    @Override
    public Object[] getAll() {
        return args.toArray();
    }
}
