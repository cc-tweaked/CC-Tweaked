// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import cc.tweaked.CCTweaked;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaValues;
import org.squiddev.cobalt.*;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Level;

import static org.squiddev.cobalt.Constants.NAME;

final class VarargArguments extends IArguments {
    private static final VarargArguments EMPTY = new VarargArguments(Constants.NONE);
    private static boolean reportedIllegalGet;

    static {
        EMPTY.escapes = EMPTY.closed = true;
    }

    private final Varargs varargs;

    private volatile boolean closed;
    private final VarargArguments root;

    private ArraySlice<Object> cache;
    private ArraySlice<String> typeNames;

    private boolean escapes;

    private VarargArguments(Varargs varargs) {
        this.varargs = varargs;
        root = this;
    }

    private VarargArguments(Varargs varargs, VarargArguments root, int offset) {
        this.varargs = varargs;
        this.root = root;
        escapes = root.escapes;
        cache = root.cache == null ? null : root.cache.drop(offset);
        typeNames = root.typeNames == null ? null : root.typeNames.drop(offset);
    }

    static VarargArguments of(Varargs values) {
        return values == Constants.NONE ? EMPTY : new VarargArguments(values);
    }

    boolean isClosed() {
        return root.closed;
    }

    private void checkAccessible() {
        if (isClosed() && !escapes) throwInaccessible();
    }

    private void throwInaccessible() {
        IllegalStateException error = new IllegalStateException("Function arguments have escaped their original scope.");
        if (!reportedIllegalGet) {
            reportedIllegalGet = true;
            CCTweaked.LOG.log(Level.SEVERE,
                "A function attempted to access arguments outside the scope of the original function. This is probably " +
                    "caused by the function scheduling work on the main thread. You may need to call IArguments.escapes().",
                error
            );
        }

        throw error;
    }

    @Override
    public int count() {
        return varargs.count();
    }

    @Override
    public Object get(int index) {
        checkAccessible();
        if (index < 0 || index >= varargs.count()) return null;

        ArraySlice<Object> cache = this.cache;
        if (cache == null) {
            cache = this.cache = new ArraySlice<>(new Object[varargs.count()], 0);
        } else {
            Object existing = cache.get(index);
            if (existing != null) return existing;
        }

        LuaValue arg = varargs.arg(index + 1);

        // This holds as either a) the arguments are not closed or b) the arguments were escaped, in which case
        // tables should have been converted already.
        assert !isClosed() || !(arg instanceof LuaTable) : "Converting a LuaTable after arguments were closed.";

        Object converted = CobaltLuaMachine.toObject(arg, null);
        cache.set(index, converted);
        return converted;
    }

    @Override
    public String getStringCoerced(int index) {
        checkAccessible();
        // This doesn't run __tostring, which is _technically_ wrong, but avoids a lot of complexity.
        return varargs.arg(index + 1).toString();
    }

    @Override
    public String getType(int index) {
        checkAccessible();

        LuaValue value = varargs.arg(index + 1);

        // If we've escaped, read it from the precomputed list, otherwise get the custom name.
        String name = escapes ? (typeNames == null ? null : typeNames.get(index)) : getCustomType(value);
        if (name != null) return name;

        return value.typeName();
    }

    @Override
    public IArguments drop(int count) {
        if (count < 0) throw new IllegalStateException("count cannot be negative");
        if (count == 0) return this;

        Varargs newArgs = varargs.subargs(count + 1);
        if (newArgs == Constants.NONE) return EMPTY;
        return new VarargArguments(newArgs, this, count);
    }

    @Override
    public double getDouble(int index) throws LuaException {
        checkAccessible();
        LuaValue value = varargs.arg(index + 1);
        if (!(value instanceof LuaNumber)) throw LuaValues.badArgument(index, "number", value.typeName());
        return value.toDouble();
    }

    @Override
    public long getLong(int index) throws LuaException {
        checkAccessible();
        LuaValue value = varargs.arg(index + 1);
        if (!(value instanceof LuaNumber)) throw LuaValues.badArgument(index, "number", value.typeName());
        return value instanceof LuaInteger ? value.toInteger() : (long) LuaValues.checkFinite(index, value.toDouble());
    }

    @Override
    public ByteBuffer getBytes(int index) throws LuaException {
        checkAccessible();
        LuaValue value = varargs.arg(index + 1);
        if (!(value instanceof LuaString)) throw LuaValues.badArgument(index, "string", value.typeName());
        return ((LuaString) value).toBuffer();
    }

    @Override
    public Optional<ByteBuffer> optBytes(int index) throws LuaException {
        checkAccessible();
        LuaValue value = varargs.arg(index + 1);
        if (value.isNil()) return Optional.empty();
        if (!(value instanceof LuaString)) throw LuaValues.badArgument(index, "string", value.typeName());
        return Optional.of(((LuaString) value).toBuffer());
    }

    @Override
    public IArguments escapes() {
        if (escapes) return this;

        ArraySlice<Object> cache = this.cache;
        ArraySlice<String> typeNames = this.typeNames;

        for (int i = 0, count = varargs.count(); i < count; i++) {
            LuaValue arg = varargs.arg(i + 1);

            // Convert tables.
            if (arg instanceof LuaTable) {
                if (cache == null) cache = new ArraySlice<>(new Object[count], 0);
                cache.set(i, CobaltLuaMachine.toObject(arg, null));
            }

            // Fetch custom type names.
            String typeName = getCustomType(arg);
            if (typeName != null) {
                if (typeNames == null) typeNames = new ArraySlice<>(new String[count], 0);
                typeNames.set(i, typeName);
            }
        }

        escapes = true;
        this.cache = cache;
        this.typeNames = typeNames;
        return this;
    }

    void close() {
        closed = true;
    }

    private static String getCustomType(LuaValue arg) {
        if (!(arg instanceof LuaTable) && !(arg instanceof LuaUserdata)) return null;

        LuaTable metatable = arg.getMetatable(null);
        return metatable != null && metatable.rawget(NAME) instanceof LuaString ? ((LuaString) metatable.rawget(NAME)).toString() : null;
    }

    private static class ArraySlice<T> {
        private final T[] array;
        private final int offset;

        private ArraySlice(T[] array, int offset) {
            this.array = array;
            this.offset = offset;
        }

        T get(int index) {
            return array[offset + index];
        }

        void set(int index, T value) {
            array[offset + index] = value;
        }

        ArraySlice<T> drop(int count) {
            return new ArraySlice<>(array, offset + count);
        }
    }
}
