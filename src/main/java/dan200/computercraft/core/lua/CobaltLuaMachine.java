// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.lua;

import cc.tweaked.CCTweaked;
import cpw.mods.fml.common.Loader;
import dan200.ComputerCraft;
import dan200.computer.core.ILuaAPI;
import dan200.computer.core.ILuaMachine;
import dan200.computer.core.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.asm.Methods;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.lib.Bit32Lib;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

public class CobaltLuaMachine implements ILuaMachine {
    private LuaState state;
    private LuaTable globals;

    private LuaThread mainRoutine = null;
    private String eventFilter = null;

    private volatile boolean isSoftAborted;
    private volatile boolean isHardAborted;
    private boolean thrownSoftAbort;

    public CobaltLuaMachine() {
        // Create an environment to run in
        LuaState state = this.state = LuaState.builder()
            .interruptHandler(() -> {
                if (isHardAborted || CobaltLuaMachine.this.state == null) throw new HardAbortError();
                if (isSoftAborted && !thrownSoftAbort) {
                    thrownSoftAbort = true;
                    throw new LuaError("Too long without yielding");
                }

                return InterruptAction.CONTINUE;
            })
            .errorReporter((e, m) -> CCTweaked.LOG.log(Level.SEVERE, "Error occurred in Lua VM. Execution will continue:\n" + m.get(), e))
            .build();

        globals = state.globals();

        try {
            CoreLibraries.debugGlobals(state);
            Bit32Lib.add(state, globals);

            globals.rawset("_HOST", valueOf("ComputerCraft " + ComputerCraft.getVersion() + " (" + Loader.instance().getMCVersionString() + ")"));
            globals.rawset("_CC_DEFAULT_SETTINGS", valueOf(""));
        } catch (LuaError e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Override
    public void addAPI(ILuaAPI api) {
        // Add the methods of an API to the global table
        LuaTable table = wrapLuaObject(api);
        String[] names = api.getNames();
        for (String name : names) {
            globals.rawset(name, table);
        }
    }

    @Override
    public void loadBios(InputStream bios) {
        // Begin executing a file (ie, the bios)
        if (mainRoutine != null) return;

        try {
            LuaFunction value = LoadState.load(state, bios, "@bios.lua", globals);
            mainRoutine = new LuaThread(state, value);
        } catch (Exception e) {
            CCTweaked.LOG.log(Level.SEVERE, "Failed to load bios.lua", e);
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
            Varargs resumeArgs = Constants.NONE;
            if (eventName != null) {
                resumeArgs = varargsOf(valueOf(eventName), toValues(arguments));
            }

            // Resume the current thread, or the main one when first starting off.
            LuaThread thread = state.getCurrentThread();
            if (thread == null || thread == state.getMainThread()) thread = mainRoutine;

            Varargs results = LuaThread.run(thread, resumeArgs);
            if (isHardAborted) throw HardAbortError.INSTANCE;
            if (results == null) return;

            LuaValue filter = results.first();
            eventFilter = filter.isString() ? filter.toString() : null;

            if (!mainRoutine.isAlive()) unload();
        } catch (HardAbortError e) {
            unload();
        } catch (LuaError e) {
            unload();
            CCTweaked.LOG.log(Level.WARNING, "Top level coroutine errored: ", e);
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

    private LuaTable wrapLuaObject(Object value) {
        LuaTable table = new LuaTable();

        Methods.forEachMethod(Methods.LUA_METHOD, value,
            (instance, method) -> table.rawset(method.getName(), new BasicFunction(this, method.getMethod(), instance, method.getName()))
        );

        if (!(value instanceof ILuaObject)) {
            try {
                if (table.next(Constants.NIL).first().isNil()) return null;
            } catch (LuaError ignored) {
                // next should never throw on nil.
            }
            return table;
        }

        ILuaObject object = (ILuaObject) value;

        String[] methods = object.getMethodNames();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i] == null) continue;

            final int method = i;
            table.rawset(methods[i], new VarArgFunction() {
                @Override
                public Varargs invoke(final LuaState state, Varargs args) throws LuaError {
                    int count = args.count();
                    Object[] objects = new Object[count];
                    for (int n = 1; n <= count; n++) objects[n - 1] = toObject(args.arg(n), null);

                    Object[] results;
                    try {
                        results = object.callMethod(method, objects);
                    } catch (Exception e) {
                        if (!(e instanceof LuaException)) e.printStackTrace();
                        throw new LuaError(e.getMessage());
                    } catch (Throwable t) {
                        throw new LuaError("Java Exception Thrown: " + t, 0);
                    }
                    return toValues(results);
                }
            });
        }
        return table;
    }

    private LuaValue toValue(Object object, Map<Object, LuaValue> values) throws LuaError {
        if (object == null) return Constants.NIL;
        if (object instanceof Number) return valueOf(((Number) object).doubleValue());
        if (object instanceof Boolean) return valueOf((Boolean) object);
        if (object instanceof String) return valueOf(object.toString());
        if (object instanceof byte[]) {
            byte[] b = (byte[]) object;
            return valueOf(Arrays.copyOf(b, b.length));
        }

        LuaValue result = values.get(object);
        if (result != null) return result;

        if (object instanceof Map) {
            LuaTable table = new LuaTable();
            values.put(object, table);

            for (Map.Entry<?, ?> pair : ((Map<?, ?>) object).entrySet()) {
                LuaValue key = toValue(pair.getKey(), values);
                LuaValue value = toValue(pair.getValue(), values);
                if (!key.isNil() && !value.isNil()) table.rawset(key, value);
            }
            return table;
        }

        if (object instanceof Collection) {
            Collection<?> objects = (Collection<?>) object;
            LuaTable table = new LuaTable(objects.size(), 0);
            values.put(object, table);
            int i = 0;
            for (Object child : objects) table.rawset(++i, toValue(child, values));
            return table;
        }

        if (object instanceof Object[]) {
            Object[] objects = (Object[]) object;
            LuaTable table = new LuaTable(objects.length, 0);
            values.put(object, table);
            for (int i = 0; i < objects.length; i++) table.rawset(i + 1, toValue(objects[i], values));
            return table;
        }

        LuaTable wrapped = wrapLuaObject(object);
        if (wrapped != null) {
            values.put(object, wrapped);
            return wrapped;
        }

        CCTweaked.LOG.warning(String.format("Received unknown type '{}', returning nil.", object.getClass().getName()));

        return Constants.NIL;
    }

    Varargs toValues(Object[] objects) throws LuaError {
        if (objects == null || objects.length == 0) return Constants.NONE;

        Map<Object, LuaValue> result = new IdentityHashMap<>(0);
        LuaValue[] values = new LuaValue[objects.length];
        for (int i = 0; i < values.length; i++) {
            Object object = objects[i];
            values[i] = toValue(object, result);
        }
        return varargsOf(values);
    }

    static Object toObject(LuaValue value, Map<LuaValue, Object> objects) {
        switch (value.type()) {
            case Constants.TNIL:
                return null;
            case Constants.TINT:
            case Constants.TNUMBER:
                return value.toDouble();
            case Constants.TBOOLEAN:
                return value.toBoolean();
            case Constants.TSTRING:
                return value.toString();
            case Constants.TTABLE: {
                // Table:
                // Start remembering stuff
                if (objects == null) {
                    objects = new IdentityHashMap<>();
                } else if (objects.containsKey(value)) {
                    return objects.get(value);
                }
                Map<Object, Object> table = new HashMap<Object, Object>();
                objects.put(value, table);

                LuaTable luaTable = (LuaTable) value;

                // Convert all keys
                LuaValue k = Constants.NIL;
                while (true) {
                    Varargs keyValue;
                    try {
                        keyValue = luaTable.next(k);
                    } catch (LuaError luaError) {
                        break;
                    }
                    k = keyValue.first();
                    if (k.isNil()) {
                        break;
                    }

                    LuaValue v = keyValue.arg(2);
                    Object keyObject = toObject(k, objects);
                    Object valueObject = toObject(v, objects);
                    if (keyObject != null && valueObject != null) {
                        table.put(keyObject, valueObject);
                    }
                }
                return table;
            }
            default:
                return null;
        }
    }

    private static final class HardAbortError extends Error {
        private static final long serialVersionUID = 7954092008586367501L;

        static final HardAbortError INSTANCE = new HardAbortError();

        private HardAbortError() {
            super("Hard Abort");
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}
