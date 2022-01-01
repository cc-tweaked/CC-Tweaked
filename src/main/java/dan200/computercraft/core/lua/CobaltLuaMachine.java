/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.asm.LuaMethod;
import dan200.computercraft.core.asm.ObjectSource;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.util.ThreadUtils;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.*;
import org.squiddev.cobalt.lib.platform.VoidResourceManipulator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKED;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKYIELD;

public class CobaltLuaMachine implements ILuaMachine
{
    private static final ThreadPoolExecutor COROUTINES = new ThreadPoolExecutor(
        0, Integer.MAX_VALUE,
        5L, TimeUnit.MINUTES,
        new SynchronousQueue<>(),
        ThreadUtils.factory( "Coroutine" )
    );

    private static final LuaMethod FUNCTION_METHOD = ( target, context, args ) -> ((ILuaFunction) target).call( args );

    private final Computer computer;
    private final TimeoutState timeout;
    private final TimeoutDebugHandler debug;
    private final ILuaContext context;

    private LuaState state;
    private LuaTable globals;

    private LuaThread mainRoutine = null;
    private String eventFilter = null;

    public CobaltLuaMachine( Computer computer, TimeoutState timeout )
    {
        this.computer = computer;
        this.timeout = timeout;
        context = new LuaContext( computer );
        debug = new TimeoutDebugHandler();

        // Create an environment to run in
        LuaState state = this.state = LuaState.builder()
            .resourceManipulator( new VoidResourceManipulator() )
            .debug( debug )
            .coroutineExecutor( command -> {
                Tracking.addValue( this.computer, TrackingField.COROUTINES_CREATED, 1 );
                COROUTINES.execute( () -> {
                    try
                    {
                        command.run();
                    }
                    finally
                    {
                        Tracking.addValue( this.computer, TrackingField.COROUTINES_DISPOSED, 1 );
                    }
                } );
            } )
            .build();

        globals = new LuaTable();
        state.setupThread( globals );

        // Add basic libraries
        globals.load( state, new BaseLib() );
        globals.load( state, new TableLib() );
        globals.load( state, new StringLib() );
        globals.load( state, new MathLib() );
        globals.load( state, new CoroutineLib() );
        globals.load( state, new Bit32Lib() );
        globals.load( state, new Utf8Lib() );
        globals.load( state, new DebugLib() );

        // Remove globals we don't want to expose
        globals.rawset( "collectgarbage", Constants.NIL );
        globals.rawset( "dofile", Constants.NIL );
        globals.rawset( "loadfile", Constants.NIL );
        globals.rawset( "print", Constants.NIL );

        // Add version globals
        globals.rawset( "_VERSION", valueOf( "Lua 5.1" ) );
        globals.rawset( "_HOST", valueOf( computer.getAPIEnvironment().getComputerEnvironment().getHostString() ) );
        globals.rawset( "_CC_DEFAULT_SETTINGS", valueOf( ComputerCraft.defaultComputerSettings ) );
        if( ComputerCraft.disableLua51Features )
        {
            globals.rawset( "_CC_DISABLE_LUA51_FEATURES", Constants.TRUE );
        }
    }

    @Override
    public void addAPI( @Nonnull ILuaAPI api )
    {
        // Add the methods of an API to the global table
        LuaTable table = wrapLuaObject( api );
        if( table == null )
        {
            ComputerCraft.log.warn( "API {} does not provide any methods", api );
            table = new LuaTable();
        }

        String[] names = api.getNames();
        for( String name : names ) globals.rawset( name, table );
    }

    @Override
    public MachineResult loadBios( @Nonnull InputStream bios )
    {
        // Begin executing a file (ie, the bios)
        if( mainRoutine != null ) return MachineResult.OK;

        try
        {
            LuaFunction value = LoadState.load( state, bios, "@bios.lua", globals );
            mainRoutine = new LuaThread( state, value, globals );
            return MachineResult.OK;
        }
        catch( CompileException e )
        {
            close();
            return MachineResult.error( e );
        }
        catch( Exception e )
        {
            ComputerCraft.log.warn( "Could not load bios.lua", e );
            close();
            return MachineResult.GENERIC_ERROR;
        }
    }

    @Override
    public MachineResult handleEvent( String eventName, Object[] arguments )
    {
        if( mainRoutine == null ) return MachineResult.OK;

        if( eventFilter != null && eventName != null && !eventName.equals( eventFilter ) && !eventName.equals( "terminate" ) )
        {
            return MachineResult.OK;
        }

        // If the soft abort has been cleared then we can reset our flag.
        timeout.refresh();
        if( !timeout.isSoftAborted() ) debug.thrownSoftAbort = false;

        try
        {
            Varargs resumeArgs = Constants.NONE;
            if( eventName != null )
            {
                resumeArgs = varargsOf( valueOf( eventName ), toValues( arguments ) );
            }

            // Resume the current thread, or the main one when first starting off.
            LuaThread thread = state.getCurrentThread();
            if( thread == null || thread == state.getMainThread() ) thread = mainRoutine;

            Varargs results = LuaThread.run( thread, resumeArgs );
            if( timeout.isHardAborted() ) throw HardAbortError.INSTANCE;
            if( results == null ) return MachineResult.PAUSE;

            LuaValue filter = results.first();
            eventFilter = filter.isString() ? filter.toString() : null;

            if( mainRoutine.getStatus().equals( "dead" ) )
            {
                close();
                return MachineResult.GENERIC_ERROR;
            }
            else
            {
                return MachineResult.OK;
            }
        }
        catch( HardAbortError | InterruptedException e )
        {
            close();
            return MachineResult.TIMEOUT;
        }
        catch( LuaError e )
        {
            close();
            ComputerCraft.log.warn( "Top level coroutine errored", e );
            return MachineResult.error( e );
        }
    }

    @Override
    public void close()
    {
        LuaState state = this.state;
        if( state == null ) return;

        state.abandon();
        mainRoutine = null;
        this.state = null;
        globals = null;
    }

    @Nullable
    private LuaTable wrapLuaObject( Object object )
    {
        String[] dynamicMethods = object instanceof IDynamicLuaObject
            ? Objects.requireNonNull( ((IDynamicLuaObject) object).getMethodNames(), "Methods cannot be null" )
            : LuaMethod.EMPTY_METHODS;

        LuaTable table = new LuaTable();
        for( int i = 0; i < dynamicMethods.length; i++ )
        {
            String method = dynamicMethods[i];
            table.rawset( method, new ResultInterpreterFunction( this, LuaMethod.DYNAMIC.get( i ), object, context, method ) );
        }

        ObjectSource.allMethods( LuaMethod.GENERATOR, object, ( instance, method ) ->
            table.rawset( method.getName(), method.nonYielding()
                ? new BasicFunction( this, method.getMethod(), instance, context, method.getName() )
                : new ResultInterpreterFunction( this, method.getMethod(), instance, context, method.getName() ) ) );

        try
        {
            if( table.keyCount() == 0 ) return null;
        }
        catch( LuaError ignored )
        {
        }

        return table;
    }

    @Nonnull
    private LuaValue toValue( @Nullable Object object, @Nullable Map<Object, LuaValue> values )
    {
        if( object == null ) return Constants.NIL;
        if( object instanceof Number ) return valueOf( ((Number) object).doubleValue() );
        if( object instanceof Boolean ) return valueOf( (Boolean) object );
        if( object instanceof String ) return valueOf( object.toString() );
        if( object instanceof byte[] b )
        {
            return valueOf( Arrays.copyOf( b, b.length ) );
        }
        if( object instanceof ByteBuffer b )
        {
            byte[] bytes = new byte[b.remaining()];
            b.get( bytes );
            return valueOf( bytes );
        }

        if( values == null ) values = new IdentityHashMap<>( 1 );
        LuaValue result = values.get( object );
        if( result != null ) return result;

        if( object instanceof ILuaFunction )
        {
            return new ResultInterpreterFunction( this, FUNCTION_METHOD, object, context, object.toString() );
        }

        if( object instanceof IDynamicLuaObject )
        {
            LuaValue wrapped = wrapLuaObject( object );
            if( wrapped == null ) wrapped = new LuaTable();
            values.put( object, wrapped );
            return wrapped;
        }

        if( object instanceof Map )
        {
            LuaTable table = new LuaTable();
            values.put( object, table );

            for( Map.Entry<?, ?> pair : ((Map<?, ?>) object).entrySet() )
            {
                LuaValue key = toValue( pair.getKey(), values );
                LuaValue value = toValue( pair.getValue(), values );
                if( !key.isNil() && !value.isNil() ) table.rawset( key, value );
            }
            return table;
        }

        if( object instanceof Collection<?> objects )
        {
            LuaTable table = new LuaTable( objects.size(), 0 );
            values.put( object, table );
            int i = 0;
            for( Object child : objects ) table.rawset( ++i, toValue( child, values ) );
            return table;
        }

        if( object instanceof Object[] objects )
        {
            LuaTable table = new LuaTable( objects.length, 0 );
            values.put( object, table );
            for( int i = 0; i < objects.length; i++ ) table.rawset( i + 1, toValue( objects[i], values ) );
            return table;
        }

        LuaTable wrapped = wrapLuaObject( object );
        if( wrapped != null )
        {
            values.put( object, wrapped );
            return wrapped;
        }

        if( ComputerCraft.logComputerErrors )
        {
            ComputerCraft.log.warn( "Received unknown type '{}', returning nil.", object.getClass().getName() );
        }
        return Constants.NIL;
    }

    Varargs toValues( Object[] objects )
    {
        if( objects == null || objects.length == 0 ) return Constants.NONE;
        if( objects.length == 1 ) return toValue( objects[0], null );

        Map<Object, LuaValue> result = new IdentityHashMap<>( 0 );
        LuaValue[] values = new LuaValue[objects.length];
        for( int i = 0; i < values.length; i++ )
        {
            Object object = objects[i];
            values[i] = toValue( object, result );
        }
        return varargsOf( values );
    }

    static Object toObject( LuaValue value, Map<LuaValue, Object> objects )
    {
        switch( value.type() )
        {
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
            case Constants.TTABLE:
            {
                // Table:
                // Start remembering stuff
                if( objects == null )
                {
                    objects = new IdentityHashMap<>( 1 );
                }
                else
                {
                    Object existing = objects.get( value );
                    if( existing != null ) return existing;
                }
                Map<Object, Object> table = new HashMap<>();
                objects.put( value, table );

                LuaTable luaTable = (LuaTable) value;

                // Convert all keys
                LuaValue k = Constants.NIL;
                while( true )
                {
                    Varargs keyValue;
                    try
                    {
                        keyValue = luaTable.next( k );
                    }
                    catch( LuaError luaError )
                    {
                        break;
                    }
                    k = keyValue.first();
                    if( k.isNil() ) break;

                    LuaValue v = keyValue.arg( 2 );
                    Object keyObject = toObject( k, objects );
                    Object valueObject = toObject( v, objects );
                    if( keyObject != null && valueObject != null )
                    {
                        table.put( keyObject, valueObject );
                    }
                }
                return table;
            }
            default:
                return null;
        }
    }

    static Object[] toObjects( Varargs values )
    {
        int count = values.count();
        Object[] objects = new Object[count];
        for( int i = 0; i < count; i++ ) objects[i] = toObject( values.arg( i + 1 ), null );
        return objects;
    }

    static IArguments toArguments( Varargs values )
    {
        return values == Constants.NONE ? VarargArguments.EMPTY : new VarargArguments( values );
    }

    /**
     * A {@link DebugHandler} which observes the {@link TimeoutState} and responds accordingly.
     */
    private class TimeoutDebugHandler extends DebugHandler
    {
        private final TimeoutState timeout;
        private int count = 0;
        boolean thrownSoftAbort;

        private boolean isPaused;
        private int oldFlags;
        private boolean oldInHook;

        TimeoutDebugHandler()
        {
            timeout = CobaltLuaMachine.this.timeout;
        }

        @Override
        public void onInstruction( DebugState ds, DebugFrame di, int pc ) throws LuaError, UnwindThrowable
        {
            di.pc = pc;

            if( isPaused ) resetPaused( ds, di );

            // We check our current pause/abort state every 128 instructions.
            if( (count = (count + 1) & 127) == 0 )
            {
                // If we've been hard aborted or closed then abort.
                if( timeout.isHardAborted() || state == null ) throw HardAbortError.INSTANCE;

                timeout.refresh();
                if( timeout.isPaused() )
                {
                    // Preserve the current state
                    isPaused = true;
                    oldInHook = ds.inhook;
                    oldFlags = di.flags;

                    // Suspend the state. This will probably throw, but we need to handle the case where it won't.
                    di.flags |= FLAG_HOOKYIELD | FLAG_HOOKED;
                    LuaThread.suspend( ds.getLuaState() );
                    resetPaused( ds, di );
                }

                handleSoftAbort();
            }

            super.onInstruction( ds, di, pc );
        }

        @Override
        public void poll() throws LuaError
        {
            // If we've been hard aborted or closed then abort.
            LuaState state = CobaltLuaMachine.this.state;
            if( timeout.isHardAborted() || state == null ) throw HardAbortError.INSTANCE;

            timeout.refresh();
            if( timeout.isPaused() ) LuaThread.suspendBlocking( state );
            handleSoftAbort();
        }

        private void resetPaused( DebugState ds, DebugFrame di )
        {
            // Restore the previous paused state
            isPaused = false;
            ds.inhook = oldInHook;
            di.flags = oldFlags;
        }

        private void handleSoftAbort() throws LuaError
        {
            // If we already thrown our soft abort error then don't do it again.
            if( !timeout.isSoftAborted() || thrownSoftAbort ) return;

            thrownSoftAbort = true;
            throw new LuaError( TimeoutState.ABORT_MESSAGE );
        }
    }

    private static final class HardAbortError extends Error
    {
        private static final long serialVersionUID = 7954092008586367501L;

        static final HardAbortError INSTANCE = new HardAbortError();

        private HardAbortError()
        {
            super( "Hard Abort", null, true, false );
        }
    }
}
