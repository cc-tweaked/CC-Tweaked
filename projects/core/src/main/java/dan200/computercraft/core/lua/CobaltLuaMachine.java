// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaFunction;
import dan200.computercraft.core.CoreConfig;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.util.LuaUtil;
import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.core.util.SanitisedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.lib.Bit32Lib;
import org.squiddev.cobalt.lib.CoreLibraries;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.Serial;
import java.nio.ByteBuffer;
import java.util.*;

public class CobaltLuaMachine implements ILuaMachine {
    private static final Logger LOG = LoggerFactory.getLogger(CobaltLuaMachine.class);

    private static final LuaMethod FUNCTION_METHOD = (target, context, args) -> ((ILuaFunction) target).call(args);

    private final TimeoutState timeout;
    private final Runnable timeoutListener = this::updateTimeout;
    private final ILuaContext context;
    private final MethodSupplier<LuaMethod> luaMethods;

    private final LuaState state;
    private final LuaThread mainRoutine;

    private volatile boolean isDisposed = false;
    private boolean thrownSoftAbort;

    private @Nullable String eventFilter = null;

    public CobaltLuaMachine(MachineEnvironment environment, InputStream bios) throws MachineException {
        timeout = environment.timeout();
        context = environment.context();
        luaMethods = environment.luaMethods();

        // Create an environment to run in
        var state = this.state = LuaState.builder()
            .interruptHandler(() -> {
                if (timeout.isHardAborted() || isDisposed) throw new HardAbortError();
                if (timeout.isSoftAborted() && !thrownSoftAbort) {
                    thrownSoftAbort = true;
                    throw new LuaError(TimeoutState.ABORT_MESSAGE);
                }

                return timeout.isPaused() ? InterruptAction.SUSPEND : InterruptAction.CONTINUE;
            })
            .errorReporter((e, msg) -> {
                if (LOG.isErrorEnabled(Logging.VM_ERROR)) {
                    LOG.error(Logging.VM_ERROR, "Error occurred in the Lua runtime. Computer will continue to execute:\n{}", msg.get(), e);
                }
            })
            .build();

        // Set up our global table.
        try {
            var globals = state.globals();
            CoreLibraries.debugGlobals(state);
            Bit32Lib.add(state, globals);
            globals.rawset("_HOST", ValueFactory.valueOf(environment.hostString()));
            globals.rawset("_CC_DEFAULT_SETTINGS", ValueFactory.valueOf(CoreConfig.defaultComputerSettings));

            // Add default APIs
            for (var api : environment.apis()) addAPI(state, globals, api);

            // And load the BIOS
            var value = LoadState.load(state, bios, "@bios.lua", globals);
            mainRoutine = new LuaThread(state, value);
        } catch (LuaError | CompileException e) {
            throw new MachineException(Nullability.assertNonNull(e.getMessage()));
        }

        timeout.addListener(timeoutListener);
    }

    private void addAPI(LuaState state, LuaTable globals, ILuaAPI api) throws LuaError {
        // Add the methods of an API to the global table
        var table = wrapLuaObject(api);
        if (table == null) {
            LOG.warn("API {} does not provide any methods", api);
            table = new LuaTable();
        }

        var names = api.getNames();
        for (var name : names) globals.rawset(name, table);

        var moduleName = api.getModuleName();
        if (moduleName != null) state.registry().getSubTable(Constants.LOADED).rawset(moduleName, table);
    }

    private void updateTimeout() {
        if (isDisposed) return;
        if (!timeout.isSoftAborted()) thrownSoftAbort = false;
        if (timeout.isSoftAborted() || timeout.isPaused()) state.interrupt();
    }

    @Override
    public MachineResult handleEvent(@Nullable String eventName, @Nullable Object[] arguments) {
        if (isDisposed) throw new IllegalStateException("Machine has been closed");

        if (eventFilter != null && eventName != null && !eventName.equals(eventFilter) && !eventName.equals("terminate")) {
            return MachineResult.OK;
        }

        try {
            var resumeArgs = eventName == null ? Constants.NONE : ValueFactory.varargsOf(ValueFactory.valueOf(eventName), toValues(arguments));

            // Resume the current thread, or the main one when first starting off.
            var thread = state.getCurrentThread();
            if (thread == null || thread == state.getMainThread()) thread = mainRoutine;

            var results = LuaThread.run(thread, resumeArgs);
            if (timeout.isHardAborted()) throw new HardAbortError();
            if (results == null) return MachineResult.PAUSE;

            var filter = results.first();
            eventFilter = filter.isString() ? filter.toString() : null;

            if (!mainRoutine.isAlive()) {
                close();
                return MachineResult.GENERIC_ERROR;
            } else {
                return MachineResult.OK;
            }
        } catch (HardAbortError e) {
            close();
            return MachineResult.TIMEOUT;
        } catch (LuaError e) {
            close();
            LOG.warn("Top level coroutine errored: {}", new SanitisedError(e));
            return MachineResult.error(e);
        }
    }

    @Override
    public void printExecutionState(StringBuilder out) {
    }

    @Override
    public void close() {
        isDisposed = true;
        state.interrupt();
        timeout.removeListener(timeoutListener);
    }

    @Nullable
    private LuaTable wrapLuaObject(Object object) {
        var table = new LuaTable();
        var found = luaMethods.forEachMethod(object, (target, name, method, info) ->
            table.rawset(name, new ResultInterpreterFunction(this, method, target, context, name)));

        return found ? table : null;
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

        // Don't share singleton values, and instead convert them to a new table.
        if (LuaUtil.isSingletonCollection(object)) return new LuaTable();

        if (values == null) values = new IdentityHashMap<>(1);
        var result = values.get(object);
        if (result != null) return result;

        var wrapped = toValueWorker(object, values);
        if (wrapped == null) {
            LOG.warn(Logging.JAVA_ERROR, "Received unknown type '{}', returning nil.", object.getClass().getName());
            return Constants.NIL;
        }

        values.put(object, wrapped);
        return wrapped;
    }

    /**
     * Convert a complex Java object (such as a collection or Lua object) to a Lua value.
     * <p>
     * This is a worker function for {@link #toValue(Object, IdentityHashMap)}, which handles the actual construction
     * of values, without reading/writing from the value map.
     *
     * @param object The object to convert.
     * @param values The map of Java to Lua values.
     * @return The converted value, or {@code null} if it could not be converted.
     * @throws LuaError If the value could not be converted.
     */
    private @Nullable LuaValue toValueWorker(Object object, IdentityHashMap<Object, LuaValue> values) throws LuaError {
        if (object instanceof ILuaFunction) {
            return new ResultInterpreterFunction(this, FUNCTION_METHOD, object, context, object.toString());
        }

        if (object instanceof IDynamicLuaObject) {
            LuaValue wrapped = wrapLuaObject(object);
            if (wrapped == null) wrapped = new LuaTable();
            return wrapped;
        }

        if (object instanceof Map<?, ?> map) {
            var table = new LuaTable();
            for (var pair : map.entrySet()) {
                var key = toValue(pair.getKey(), values);
                var value = toValue(pair.getValue(), values);
                if (!key.isNil() && !value.isNil()) table.rawset(key, value);
            }
            return table;
        }

        if (object instanceof Collection<?> objects) {
            var table = new LuaTable(objects.size(), 0);
            var i = 0;
            for (var child : objects) table.rawset(++i, toValue(child, values));
            return table;
        }

        if (object instanceof Object[] objects) {
            var table = new LuaTable(objects.length, 0);
            for (var i = 0; i < objects.length; i++) table.rawset(i + 1, toValue(objects[i], values));
            return table;
        }

        return wrapLuaObject(object);
    }

    Varargs toValues(@Nullable Object[] objects) throws LuaError {
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

    @Nullable
    static Object toObject(LuaValue value, @Nullable IdentityHashMap<LuaValue, Object> objects) {
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

    static Object[] toObjects(Varargs values) {
        var count = values.count();
        var objects = new Object[count];
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
