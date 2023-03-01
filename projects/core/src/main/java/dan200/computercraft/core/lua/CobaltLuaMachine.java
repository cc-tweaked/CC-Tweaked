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
import dan200.computercraft.core.asm.LuaMethod;
import dan200.computercraft.core.asm.ObjectSource;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.core.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.lib.*;
import org.squiddev.cobalt.lib.platform.VoidResourceManipulator;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.Serial;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKED;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKYIELD;

public class CobaltLuaMachine implements ILuaMachine {
    private static final Logger LOG = LoggerFactory.getLogger(CobaltLuaMachine.class);

    private static final ThreadPoolExecutor COROUTINES = new ThreadPoolExecutor(
        0, Integer.MAX_VALUE,
        5L, TimeUnit.MINUTES,
        new SynchronousQueue<>(),
        ThreadUtils.factory("Coroutine")
    );

    private static final LuaMethod FUNCTION_METHOD = (target, context, args) -> ((ILuaFunction) target).call(args);

    private final TimeoutState timeout;
    private final TimeoutDebugHandler debug;
    private final ILuaContext context;

    private @Nullable LuaState state;
    private @Nullable LuaTable globals;

    private @Nullable LuaThread mainRoutine = null;
    private @Nullable String eventFilter = null;

    public CobaltLuaMachine(MachineEnvironment environment) {
        timeout = environment.timeout();
        context = environment.context();
        debug = new TimeoutDebugHandler();

        // Create an environment to run in
        var metrics = environment.metrics();
        var state = this.state = LuaState.builder()
            .resourceManipulator(new VoidResourceManipulator())
            .debug(debug)
            .coroutineExecutor(command -> {
                metrics.observe(Metrics.COROUTINES_CREATED);
                COROUTINES.execute(() -> {
                    try {
                        command.run();
                    } finally {
                        metrics.observe(Metrics.COROUTINES_DISPOSED);
                    }
                });
            })
            .errorReporter((e, msg) -> {
                if (LOG.isErrorEnabled(Logging.VM_ERROR)) {
                    LOG.error(Logging.VM_ERROR, "Error occurred in the Lua runtime. Computer will continue to execute:\n{}", msg.get(), e);
                }
            })
            .build();

        globals = new LuaTable();
        state.setupThread(globals);

        // Add basic libraries
        globals.load(state, new BaseLib());
        globals.load(state, new TableLib());
        globals.load(state, new StringLib());
        globals.load(state, new MathLib());
        globals.load(state, new CoroutineLib());
        globals.load(state, new Bit32Lib());
        globals.load(state, new Utf8Lib());
        globals.load(state, new DebugLib());

        // Remove globals we don't want to expose
        globals.rawset("collectgarbage", Constants.NIL);
        globals.rawset("dofile", Constants.NIL);
        globals.rawset("loadfile", Constants.NIL);
        globals.rawset("print", Constants.NIL);

        // Add version globals
        globals.rawset("_VERSION", valueOf("Lua 5.1"));
        globals.rawset("_HOST", valueOf(environment.hostString()));
        globals.rawset("_CC_DEFAULT_SETTINGS", valueOf(CoreConfig.defaultComputerSettings));
        if (CoreConfig.disableLua51Features) {
            globals.rawset("_CC_DISABLE_LUA51_FEATURES", Constants.TRUE);
        }
    }

    @Override
    public void addAPI(ILuaAPI api) {
        if (globals == null) throw new IllegalStateException("Machine has been closed");

        // Add the methods of an API to the global table
        var table = wrapLuaObject(api);
        if (table == null) {
            LOG.warn("API {} does not provide any methods", api);
            table = new LuaTable();
        }

        var names = api.getNames();
        for (var name : names) globals.rawset(name, table);
    }

    @Override
    public MachineResult loadBios(InputStream bios) {
        if (mainRoutine != null) throw new IllegalStateException("Already set up the machine");
        if (state == null || globals == null) throw new IllegalStateException("Machine has been destroyed.");

        try {
            var value = LoadState.load(state, bios, "@bios.lua", globals);
            mainRoutine = new LuaThread(state, value, globals);
            return MachineResult.OK;
        } catch (CompileException e) {
            close();
            return MachineResult.error(e);
        } catch (Exception e) {
            LOG.warn("Could not load bios.lua", e);
            close();
            return MachineResult.GENERIC_ERROR;
        }
    }

    @Override
    public MachineResult handleEvent(@Nullable String eventName, @Nullable Object[] arguments) {
        if (mainRoutine == null || state == null) throw new IllegalStateException("Machine has been closed");

        if (eventFilter != null && eventName != null && !eventName.equals(eventFilter) && !eventName.equals("terminate")) {
            return MachineResult.OK;
        }

        // If the soft abort has been cleared then we can reset our flag.
        timeout.refresh();
        if (!timeout.isSoftAborted()) debug.thrownSoftAbort = false;

        try {
            Varargs resumeArgs = Constants.NONE;
            if (eventName != null) {
                resumeArgs = varargsOf(valueOf(eventName), toValues(arguments));
            }

            // Resume the current thread, or the main one when first starting off.
            var thread = state.getCurrentThread();
            if (thread == null || thread == state.getMainThread()) thread = mainRoutine;

            var results = LuaThread.run(thread, resumeArgs);
            if (timeout.isHardAborted()) throw HardAbortError.INSTANCE;
            if (results == null) return MachineResult.PAUSE;

            var filter = results.first();
            eventFilter = filter.isString() ? filter.toString() : null;

            if (mainRoutine.getStatus().equals("dead")) {
                close();
                return MachineResult.GENERIC_ERROR;
            } else {
                return MachineResult.OK;
            }
        } catch (HardAbortError | InterruptedException e) {
            close();
            return MachineResult.TIMEOUT;
        } catch (LuaError e) {
            close();
            LOG.warn("Top level coroutine errored", e);
            return MachineResult.error(e);
        }
    }

    @Override
    public void printExecutionState(StringBuilder out) {
        var state = this.state;
        if (state == null) {
            out.append("CobaltLuaMachine is terminated\n");
        } else {
            state.printExecutionState(out);
        }
    }

    @Override
    public void close() {
        var state = this.state;
        if (state == null) return;

        state.abandon();
        mainRoutine = null;
        this.state = null;
        globals = null;
    }

    @Nullable
    private LuaTable wrapLuaObject(Object object) {
        var dynamicMethods = object instanceof IDynamicLuaObject dynamic
            ? Objects.requireNonNull(dynamic.getMethodNames(), "Methods cannot be null")
            : LuaMethod.EMPTY_METHODS;

        var table = new LuaTable();
        for (var i = 0; i < dynamicMethods.length; i++) {
            var method = dynamicMethods[i];
            table.rawset(method, new ResultInterpreterFunction(this, LuaMethod.DYNAMIC.get(i), object, context, method));
        }

        ObjectSource.allMethods(LuaMethod.GENERATOR, object, (instance, method) ->
            table.rawset(method.getName(), method.nonYielding()
                ? new BasicFunction(this, method.getMethod(), instance, context, method.getName())
                : new ResultInterpreterFunction(this, method.getMethod(), instance, context, method.getName())));

        try {
            if (table.keyCount() == 0) return null;
        } catch (LuaError ignored) {
            // next should never throw on nil.
        }

        return table;
    }

    private LuaValue toValue(@Nullable Object object, @Nullable IdentityHashMap<Object, LuaValue> values) {
        if (object == null) return Constants.NIL;
        if (object instanceof Number num) return valueOf(num.doubleValue());
        if (object instanceof Boolean bool) return valueOf(bool);
        if (object instanceof String str) return valueOf(str);
        if (object instanceof byte[] b) {
            return valueOf(Arrays.copyOf(b, b.length));
        }
        if (object instanceof ByteBuffer b) {
            var bytes = new byte[b.remaining()];
            b.get(bytes);
            return valueOf(bytes);
        }

        if (values == null) values = new IdentityHashMap<>(1);
        var result = values.get(object);
        if (result != null) return result;

        if (object instanceof ILuaFunction) {
            return new ResultInterpreterFunction(this, FUNCTION_METHOD, object, context, object.toString());
        }

        if (object instanceof IDynamicLuaObject) {
            LuaValue wrapped = wrapLuaObject(object);
            if (wrapped == null) wrapped = new LuaTable();
            values.put(object, wrapped);
            return wrapped;
        }

        if (object instanceof Map<?, ?> map) {
            var table = new LuaTable();
            values.put(object, table);

            for (Map.Entry<?, ?> pair : map.entrySet()) {
                var key = toValue(pair.getKey(), values);
                var value = toValue(pair.getValue(), values);
                if (!key.isNil() && !value.isNil()) table.rawset(key, value);
            }
            return table;
        }

        if (object instanceof Collection<?> objects) {
            var table = new LuaTable(objects.size(), 0);
            values.put(object, table);
            var i = 0;
            for (Object child : objects) table.rawset(++i, toValue(child, values));
            return table;
        }

        if (object instanceof Object[] objects) {
            var table = new LuaTable(objects.length, 0);
            values.put(object, table);
            for (var i = 0; i < objects.length; i++) table.rawset(i + 1, toValue(objects[i], values));
            return table;
        }

        var wrapped = wrapLuaObject(object);
        if (wrapped != null) {
            values.put(object, wrapped);
            return wrapped;
        }

        LOG.warn(Logging.JAVA_ERROR, "Received unknown type '{}', returning nil.", object.getClass().getName());
        return Constants.NIL;
    }

    Varargs toValues(@Nullable Object[] objects) {
        if (objects == null || objects.length == 0) return Constants.NONE;
        if (objects.length == 1) return toValue(objects[0], null);

        var result = new IdentityHashMap<Object, LuaValue>(0);
        var values = new LuaValue[objects.length];
        for (var i = 0; i < values.length; i++) {
            var object = objects[i];
            values[i] = toValue(object, result);
        }
        return varargsOf(values);
    }

    @Nullable
    static Object toObject(LuaValue value, @Nullable IdentityHashMap<LuaValue, Object> objects) {
        switch (value.type()) {
            case Constants.TNIL:
            case Constants.TNONE:
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
                    objects = new IdentityHashMap<>(1);
                } else {
                    var existing = objects.get(value);
                    if (existing != null) return existing;
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
                return table;
            }
            default:
                return null;
        }
    }

    static Object[] toObjects(Varargs values) {
        var count = values.count();
        var objects = new Object[count];
        for (var i = 0; i < count; i++) objects[i] = toObject(values.arg(i + 1), null);
        return objects;
    }

    /**
     * A {@link DebugHandler} which observes the {@link TimeoutState} and responds accordingly.
     */
    private class TimeoutDebugHandler extends DebugHandler {
        private final TimeoutState timeout;
        private int count = 0;
        boolean thrownSoftAbort;

        private boolean isPaused;
        private int oldFlags;
        private boolean oldInHook;

        TimeoutDebugHandler() {
            timeout = CobaltLuaMachine.this.timeout;
        }

        @Override
        public void onInstruction(DebugState ds, DebugFrame di, int pc) throws LuaError, UnwindThrowable {
            di.pc = pc;

            if (isPaused) resetPaused(ds, di);

            // We check our current pause/abort state every 128 instructions.
            if ((count = (count + 1) & 127) == 0) {
                if (timeout.isHardAborted() || state == null) throw HardAbortError.INSTANCE;
                if (timeout.isPaused()) handlePause(ds, di);
                if (timeout.isSoftAborted()) handleSoftAbort();
            }

            super.onInstruction(ds, di, pc);
        }

        @Override
        public void poll() throws LuaError {
            var state = CobaltLuaMachine.this.state;
            if (timeout.isHardAborted() || state == null) throw HardAbortError.INSTANCE;
            if (timeout.isPaused()) LuaThread.suspendBlocking(state);
            if (timeout.isSoftAborted()) handleSoftAbort();
        }

        private void resetPaused(DebugState ds, DebugFrame di) {
            // Restore the previous paused state
            isPaused = false;
            ds.inhook = oldInHook;
            di.flags = oldFlags;
        }

        private void handleSoftAbort() throws LuaError {
            // If we already thrown our soft abort error then don't do it again.
            if (thrownSoftAbort) return;

            thrownSoftAbort = true;
            throw new LuaError(TimeoutState.ABORT_MESSAGE);
        }

        private void handlePause(DebugState ds, DebugFrame di) throws LuaError, UnwindThrowable {
            // Preserve the current state
            isPaused = true;
            oldInHook = ds.inhook;
            oldFlags = di.flags;

            // Suspend the state. This will probably throw, but we need to handle the case where it won't.
            di.flags |= FLAG_HOOKYIELD | FLAG_HOOKED;
            LuaThread.suspend(ds.getLuaState());
            resetPaused(ds, di);
        }
    }

    private static final class HardAbortError extends Error {
        @Serial
        private static final long serialVersionUID = 7954092008586367501L;

        @SuppressWarnings("StaticAssignmentOfThrowable")
        static final HardAbortError INSTANCE = new HardAbortError();

        private HardAbortError() {
            super("Hard Abort", null, true, false);
        }
    }
}
