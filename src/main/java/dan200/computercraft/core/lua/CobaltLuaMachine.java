// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.lua;

import cpw.mods.fml.common.Loader;
import dan200.ComputerCraft;
import dan200.computer.core.ILuaAPI;
import dan200.computer.core.ILuaMachine;
import dan200.computer.core.ILuaObject;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.asm.LuaMethodSupplier;
import dan200.computercraft.core.lua.errorinfo.ErrorInfoLib;
import dan200.computercraft.core.util.LuaUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.lib.Bit32Lib;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.nio.ByteBuffer;
import java.util.*;

import static org.squiddev.cobalt.ValueFactory.valueOf;

public class CobaltLuaMachine implements ILuaMachine {
    private static final Logger LOG = LoggerFactory.getLogger(CobaltLuaMachine.class);

    private LuaState state;
    private LuaTable globals;

    private LuaThread mainRoutine = null;
    private String eventFilter = null;

    private volatile boolean isSoftAborted;
    private volatile boolean isHardAborted;
    private boolean thrownSoftAbort;

    public CobaltLuaMachine() {
        // Create an environment to run in
        var state = this.state = LuaState.builder()
            .interruptHandler(() -> {
                if (isHardAborted || this.state == null) throw new HardAbortError();
                if (isSoftAborted && !thrownSoftAbort) {
                    thrownSoftAbort = true;
                    throw new LuaError("Too long without yielding");
                }

                return InterruptAction.CONTINUE;
            })
            .errorReporter((e, msg) -> {
                if (LOG.isErrorEnabled(Logging.VM_ERROR)) {
                    LOG.error(Logging.VM_ERROR, "Error occurred in the Lua runtime. Computer will continue to execute:\n{}", msg.get(), e);
                }
            })
            .build();

        globals = state.globals();

        try {
            CoreLibraries.debugGlobals(state);
            Bit32Lib.add(state);
            ErrorInfoLib.add(state);

            globals.rawset("_HOST", valueOf("ComputerCraft " + ComputerCraft.getVersion() + " (" + Loader.instance().getMCVersionString() + ")"));
            globals.rawset("_CC_DEFAULT_SETTINGS", valueOf(""));
        } catch (LuaError e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Override
    public void addAPI(ILuaAPI api) {
        var table = new LuaTable();
        if (!makeLuaObject(api, table)) LOG.warn("API {} does not provide any methods", api);

        var names = api.getNames();
        for (var name : names) globals.rawset(name, table);
    }

    @Override
    public void loadBios(InputStream bios) {
        // Begin executing a file (ie, the bios)
        if (mainRoutine != null) return;

        try {
            LuaFunction value = LoadState.load(state, bios, "@bios.lua", globals);
            mainRoutine = new LuaThread(state, value);
        } catch (Exception e) {
            LOG.error("Failed to load bios.lua", e);
            unload();
        }
    }

    @Override
    public void handleEvent(String eventName, Object[] arguments) {
        if (mainRoutine == null) return;

        if (eventFilter != null && eventName != null && !eventName.equals(eventFilter) && !eventName.equals("terminate")) {
            return;
        }

        // If the soft abort has been cleared then we can reset our flag.
        isSoftAborted = isHardAborted = thrownSoftAbort = false;

        try {
            var resumeArgs = eventName == null ? Constants.NONE : ValueFactory.varargsOf(ValueFactory.valueOf(eventName), toValues(arguments));

            // Resume the current thread, or the main one when first starting off.
            var thread = state.getCurrentThread();
            if (thread == null || thread == state.getMainThread()) thread = mainRoutine;

            var results = LuaThread.run(thread, resumeArgs);
            if (isHardAborted) throw new HardAbortError();
            if (results == null) return;

            var filter = results.first();
            eventFilter = filter.isString() ? filter.toString() : null;

            if (!mainRoutine.isAlive()) unload();
        } catch (HardAbortError e) {
            unload();
        } catch (LuaError e) {
            unload();
            LOG.warn("Top level coroutine errored", e);
        } catch (Throwable t) {
            LOG.error("error in Lua VM", t);
            throw t;
        }
    }

    @Override
    public void softAbort(String s) {
        isSoftAborted = true;
        if (state != null) state.interrupt();
    }

    @Override
    public void hardAbort(String s) {
        isHardAborted = true;
        if (state != null) state.interrupt();
    }

    @Override
    public boolean saveState(OutputStream outputStream) {
        return false;
    }

    @Override
    public boolean restoreState(InputStream inputStream) {
        return false;
    }

    @Override
    public boolean isFinished() {
        return state == null;
    }

    @Override
    public void unload() {
        LuaState state = this.state;
        if (state == null) return;

        state.interrupt();
        mainRoutine = null;
        this.state = null;
        globals = null;
    }

    /**
     * Populate a table with methods from an object.
     *
     * @param object The object to draw methods from.
     * @param table  The table to fill.
     * @return Whether any methods were found.
     */
    private boolean makeLuaObject(Object object, LuaTable table) {
        return LuaMethodSupplier.INSTANCE.forEachMethod(object, (target, name, method, info) ->
            table.rawset(name, new ResultInterpreterFunction(this, method, target, name)));
    }

    private LuaValue toValue(@Nullable Object object, @Nullable IdentityHashMap<Object, LuaValue> values) throws LuaError {
        if (object == null) return Constants.NIL;
        if (object instanceof Number num) return ValueFactory.valueOf(num.doubleValue());
        if (object instanceof Boolean bool) return ValueFactory.valueOf(bool);
        if (object instanceof String str) return ValueFactory.valueOf(str);
        if (object instanceof byte[] b) return ValueFactory.valueOf(Arrays.copyOf(b, b.length));
        if (object instanceof ByteBuffer b) {
            var bytes = new byte[b.remaining()];
            b.get(bytes);
            return ValueFactory.valueOf(bytes);
        }

        // We have a more complex object, which is possibly recursive. First look up our object in the lookup map,
        // and reuse it if present.
        if (values == null) values = new IdentityHashMap<>(1);
        var result = values.get(object);
        if (result != null) return result;

        if (object instanceof ILuaObject) {
            var table = new LuaTable();
            makeLuaObject(object, table);
            values.put(object, table);
            return table;
        }

        // The following objects may be recursive. In these instances, we need to be careful to store the value *before*
        // recursing, to avoid stack overflows.

        if (object instanceof Map<?, ?> map) {
            // Don't share singleton values, and instead convert them to a new table.
            if (LuaUtil.isSingletonMap(map)) return new LuaTable();

            var table = new LuaTable();
            values.put(object, table);

            for (var pair : map.entrySet()) {
                var key = toValue(pair.getKey(), values);
                var value = toValue(pair.getValue(), values);
                if (!key.isNil() && !value.isNil()) table.rawset(key, value);
            }
            return table;
        }

        if (object instanceof Collection<?> objects) {
            // Don't share singleton values, and instead convert them to a new table.
            if (LuaUtil.isSingletonCollection(objects)) return new LuaTable();

            var table = new LuaTable(objects.size(), 0);
            values.put(object, table);

            var i = 0;
            for (var child : objects) table.rawset(++i, toValue(child, values));
            return table;
        }

        if (object instanceof Object[] objects) {
            var table = new LuaTable(objects.length, 0);
            values.put(object, table);

            for (var i = 0; i < objects.length; i++) table.rawset(i + 1, toValue(objects[i], values));
            return table;
        }

        var table = new LuaTable();
        if (makeLuaObject(object, table)) {
            values.put(object, table);
            return table;
        }

        LOG.warn(Logging.JAVA_ERROR, "Received unknown type '{}', returning nil.", object.getClass().getName());
        return Constants.NIL;
    }

    Varargs toValues(@Nullable Object @Nullable [] objects) throws LuaError {
        if (objects == null || objects.length == 0) return Constants.NONE;
        if (objects.length == 1) return toValue(objects[0], null);

        var result = new IdentityHashMap<Object, LuaValue>(0);
        var values = new LuaValue[objects.length];
        for (var i = 0; i < values.length; i++) {
            var object = objects[i];
            values[i] = toValue(object, result);
        }
        return ValueFactory.varargsOf(values);
    }

    static @Nullable Object toObject(LuaValue value, @Nullable IdentityHashMap<LuaValue, Object> objects) {
        return switch (value.type()) {
            case Constants.TNIL -> null;
            case Constants.TINT, Constants.TNUMBER -> value.toDouble();
            case Constants.TBOOLEAN -> value.toBoolean();
            case Constants.TSTRING -> value.toString();
            case Constants.TTABLE -> {
                // Table:
                // Start remembering stuff
                if (objects == null) {
                    objects = new IdentityHashMap<>(1);
                } else {
                    var existing = objects.get(value);
                    if (existing != null) yield existing;
                }
                Map<Object, Object> table = new HashMap<>();
                objects.put(value, table);

                var luaTable = (LuaTable) value;

                // Convert all keys
                var k = Constants.NIL;
                while (true) {
                    Varargs keyValue;
                    try {
                        keyValue = luaTable.next(k);
                    } catch (LuaError luaError) {
                        break;
                    }
                    k = keyValue.first();
                    if (k.isNil()) break;

                    var v = keyValue.arg(2);
                    var keyObject = toObject(k, objects);
                    var valueObject = toObject(v, objects);
                    if (keyObject != null && valueObject != null) {
                        table.put(keyObject, valueObject);
                    }
                }
                yield table;
            }
            default -> null;
        };
    }

    static @Nullable Object[] toObjects(Varargs values) {
        var count = values.count();
        var objects = new @Nullable Object[count];
        for (var i = 0; i < count; i++) objects[i] = toObject(values.arg(i + 1), null);
        return objects;
    }

    private static final class HardAbortError extends Error {
        @Serial
        private static final long serialVersionUID = 7954092008586367501L;

        private HardAbortError() {
            super("Hard Abort");
        }
    }
}
