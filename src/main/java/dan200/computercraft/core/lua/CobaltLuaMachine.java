/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.asm.LuaMethod;
import dan200.computercraft.core.asm.ObjectSource;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.util.ThreadUtils;
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
    private static final ThreadPoolExecutor COROUTINES = new ThreadPoolExecutor( 0,
        Integer.MAX_VALUE,
        5L,
        TimeUnit.MINUTES,
        new SynchronousQueue<>(),
        ThreadUtils.factory( "Coroutine" ) );

    private static final LuaMethod FUNCTION_METHOD = ( target, context, args ) -> ((ILuaFunction) target).call( args );

    private final Computer m_computer;
    private final TimeoutState timeout;
    private final TimeoutDebugHandler debug;
    private final ILuaContext context = new CobaltLuaContext();

    private LuaState m_state;
    private LuaTable m_globals;

    private LuaThread m_mainRoutine = null;
    private String m_eventFilter = null;

    public CobaltLuaMachine( Computer computer, TimeoutState timeout )
    {
        this.m_computer = computer;
        this.timeout = timeout;
        this.debug = new TimeoutDebugHandler();

        // Create an environment to run in
        LuaState state = this.m_state = LuaState.builder()
            .resourceManipulator( new VoidResourceManipulator() )
            .debug( this.debug )
            .coroutineExecutor( command -> {
                Tracking.addValue( this.m_computer, TrackingField.COROUTINES_CREATED, 1 );
                COROUTINES.execute( () -> {
                    try
                    {
                        command.run();
                    }
                    finally
                    {
                        Tracking.addValue( this.m_computer, TrackingField.COROUTINES_DISPOSED, 1 );
                    }
                } );
            } )
            .build();

        this.m_globals = new LuaTable();
        state.setupThread( this.m_globals );

        // Add basic libraries
        this.m_globals.load( state, new BaseLib() );
        this.m_globals.load( state, new TableLib() );
        this.m_globals.load( state, new StringLib() );
        this.m_globals.load( state, new MathLib() );
        this.m_globals.load( state, new CoroutineLib() );
        this.m_globals.load( state, new Bit32Lib() );
        this.m_globals.load( state, new Utf8Lib() );
        if( ComputerCraft.debugEnable )
        {
            this.m_globals.load( state, new DebugLib() );
        }

        // Remove globals we don't want to expose
        this.m_globals.rawset( "collectgarbage", Constants.NIL );
        this.m_globals.rawset( "dofile", Constants.NIL );
        this.m_globals.rawset( "loadfile", Constants.NIL );
        this.m_globals.rawset( "print", Constants.NIL );

        // Add version globals
        this.m_globals.rawset( "_VERSION", valueOf( "Lua 5.1" ) );
        this.m_globals.rawset( "_HOST",
            valueOf( computer.getAPIEnvironment()
                .getComputerEnvironment()
                .getHostString() ) );
        this.m_globals.rawset( "_CC_DEFAULT_SETTINGS", valueOf( ComputerCraft.defaultComputerSettings ) );
        if( ComputerCraft.disableLua51Features )
        {
            this.m_globals.rawset( "_CC_DISABLE_LUA51_FEATURES", Constants.TRUE );
        }
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
                    if( existing != null )
                    {
                        return existing;
                    }
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
                    if( k.isNil() )
                    {
                        break;
                    }

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
        for( int i = 0; i < count; i++ )
        {
            objects[i] = toObject( values.arg( i + 1 ), null );
        }
        return objects;
    }

    static IArguments toArguments( Varargs values )
    {
        return values == Constants.NONE ? VarargArguments.EMPTY : new VarargArguments( values );
    }

    @Override
    public void addAPI( @Nonnull ILuaAPI api )
    {
        // Add the methods of an API to the global table
        LuaTable table = this.wrapLuaObject( api );
        if( table == null )
        {
            ComputerCraft.log.warn( "API {} does not provide any methods", api );
            table = new LuaTable();
        }

        String[] names = api.getNames();
        for( String name : names )
        {
            this.m_globals.rawset( name, table );
        }
    }

    @Override
    public MachineResult loadBios( @Nonnull InputStream bios )
    {
        // Begin executing a file (ie, the bios)
        if( this.m_mainRoutine != null )
        {
            return MachineResult.OK;
        }

        try
        {
            LuaFunction value = LoadState.load( this.m_state, bios, "@bios.lua", this.m_globals );
            this.m_mainRoutine = new LuaThread( this.m_state, value, this.m_globals );
            return MachineResult.OK;
        }
        catch( CompileException e )
        {
            this.close();
            return MachineResult.error( e );
        }
        catch( Exception e )
        {
            ComputerCraft.log.warn( "Could not load bios.lua", e );
            this.close();
            return MachineResult.GENERIC_ERROR;
        }
    }

    @Override
    public MachineResult handleEvent( String eventName, Object[] arguments )
    {
        if( this.m_mainRoutine == null )
        {
            return MachineResult.OK;
        }

        if( this.m_eventFilter != null && eventName != null && !eventName.equals( this.m_eventFilter ) && !eventName.equals( "terminate" ) )
        {
            return MachineResult.OK;
        }

        // If the soft abort has been cleared then we can reset our flag.
        this.timeout.refresh();
        if( !this.timeout.isSoftAborted() )
        {
            this.debug.thrownSoftAbort = false;
        }

        try
        {
            Varargs resumeArgs = Constants.NONE;
            if( eventName != null )
            {
                resumeArgs = varargsOf( valueOf( eventName ), this.toValues( arguments ) );
            }

            // Resume the current thread, or the main one when first starting off.
            LuaThread thread = this.m_state.getCurrentThread();
            if( thread == null || thread == this.m_state.getMainThread() )
            {
                thread = this.m_mainRoutine;
            }

            Varargs results = LuaThread.run( thread, resumeArgs );
            if( this.timeout.isHardAborted() )
            {
                throw HardAbortError.INSTANCE;
            }
            if( results == null )
            {
                return MachineResult.PAUSE;
            }

            LuaValue filter = results.first();
            this.m_eventFilter = filter.isString() ? filter.toString() : null;

            if( this.m_mainRoutine.getStatus()
                .equals( "dead" ) )
            {
                this.close();
                return MachineResult.GENERIC_ERROR;
            }
            else
            {
                return MachineResult.OK;
            }
        }
        catch( HardAbortError | InterruptedException e )
        {
            this.close();
            return MachineResult.TIMEOUT;
        }
        catch( LuaError e )
        {
            this.close();
            ComputerCraft.log.warn( "Top level coroutine errored", e );
            return MachineResult.error( e );
        }
    }

    @Override
    public void close()
    {
        LuaState state = this.m_state;
        if( state == null )
        {
            return;
        }

        state.abandon();
        this.m_mainRoutine = null;
        this.m_state = null;
        this.m_globals = null;
    }

    @Nullable
    private LuaTable wrapLuaObject( Object object )
    {
        String[] dynamicMethods = object instanceof IDynamicLuaObject ? Objects.requireNonNull( ((IDynamicLuaObject) object).getMethodNames(),
            "Methods cannot be null" ) : LuaMethod.EMPTY_METHODS;

        LuaTable table = new LuaTable();
        for( int i = 0; i < dynamicMethods.length; i++ )
        {
            String method = dynamicMethods[i];
            table.rawset( method, new ResultInterpreterFunction( this, LuaMethod.DYNAMIC.get( i ), object, this.context, method ) );
        }

        ObjectSource.allMethods( LuaMethod.GENERATOR,
            object,
            ( instance, method ) -> table.rawset( method.getName(),
                method.nonYielding() ? new BasicFunction( this,
                    (LuaMethod) method.getMethod(),
                    instance, this.context,
                    method.getName() ) :
                    new ResultInterpreterFunction(
                        this,
                        (LuaMethod) method.getMethod(),
                        instance, this.context,
                        method.getName() ) ) );

        try
        {
            if( table.keyCount() == 0 )
            {
                return null;
            }
        }
        catch( LuaError ignored )
        {
        }

        return table;
    }

    @Nonnull
    private LuaValue toValue( @Nullable Object object, @Nullable Map<Object, LuaValue> values )
    {
        if( object == null )
        {
            return Constants.NIL;
        }
        if( object instanceof Number )
        {
            return valueOf( ((Number) object).doubleValue() );
        }
        if( object instanceof Boolean )
        {
            return valueOf( (Boolean) object );
        }
        if( object instanceof String )
        {
            return valueOf( object.toString() );
        }
        if( object instanceof byte[] )
        {
            byte[] b = (byte[]) object;
            return valueOf( Arrays.copyOf( b, b.length ) );
        }
        if( object instanceof ByteBuffer )
        {
            ByteBuffer b = (ByteBuffer) object;
            byte[] bytes = new byte[b.remaining()];
            b.get( bytes );
            return valueOf( bytes );
        }

        if( values == null )
        {
            values = new IdentityHashMap<>( 1 );
        }
        LuaValue result = values.get( object );
        if( result != null )
        {
            return result;
        }

        if( object instanceof ILuaFunction )
        {
            return new ResultInterpreterFunction( this, FUNCTION_METHOD, object, this.context, object.toString() );
        }

        if( object instanceof IDynamicLuaObject )
        {
            LuaValue wrapped = this.wrapLuaObject( object );
            if( wrapped == null )
            {
                wrapped = new LuaTable();
            }
            values.put( object, wrapped );
            return wrapped;
        }

        if( object instanceof Map )
        {
            LuaTable table = new LuaTable();
            values.put( object, table );

            for( Map.Entry<?, ?> pair : ((Map<?, ?>) object).entrySet() )
            {
                LuaValue key = this.toValue( pair.getKey(), values );
                LuaValue value = this.toValue( pair.getValue(), values );
                if( !key.isNil() && !value.isNil() )
                {
                    table.rawset( key, value );
                }
            }
            return table;
        }

        if( object instanceof Collection )
        {
            Collection<?> objects = (Collection<?>) object;
            LuaTable table = new LuaTable( objects.size(), 0 );
            values.put( object, table );
            int i = 0;
            for( Object child : objects )
            {
                table.rawset( ++i, this.toValue( child, values ) );
            }
            return table;
        }

        if( object instanceof Object[] )
        {
            Object[] objects = (Object[]) object;
            LuaTable table = new LuaTable( objects.length, 0 );
            values.put( object, table );
            for( int i = 0; i < objects.length; i++ )
            {
                table.rawset( i + 1, this.toValue( objects[i], values ) );
            }
            return table;
        }

        LuaTable wrapped = this.wrapLuaObject( object );
        if( wrapped != null )
        {
            values.put( object, wrapped );
            return wrapped;
        }

        if( ComputerCraft.logComputerErrors )
        {
            ComputerCraft.log.warn( "Received unknown type '{}', returning nil.",
                object.getClass()
                    .getName() );
        }
        return Constants.NIL;
    }

    Varargs toValues( Object[] objects )
    {
        if( objects == null || objects.length == 0 )
        {
            return Constants.NONE;
        }
        if( objects.length == 1 )
        {
            return this.toValue( objects[0], null );
        }

        Map<Object, LuaValue> result = new IdentityHashMap<>( 0 );
        LuaValue[] values = new LuaValue[objects.length];
        for( int i = 0; i < values.length; i++ )
        {
            Object object = objects[i];
            values[i] = this.toValue( object, result );
        }
        return varargsOf( values );
    }

    private static final class HardAbortError extends Error
    {
        static final HardAbortError INSTANCE = new HardAbortError();
        private static final long serialVersionUID = 7954092008586367501L;

        private HardAbortError()
        {
            super( "Hard Abort", null, true, false );
        }
    }

    /**
     * A {@link DebugHandler} which observes the {@link TimeoutState} and responds accordingly.
     */
    private class TimeoutDebugHandler extends DebugHandler
    {
        private final TimeoutState timeout;
        boolean thrownSoftAbort;
        private int count = 0;
        private boolean isPaused;
        private int oldFlags;
        private boolean oldInHook;

        TimeoutDebugHandler()
        {
            this.timeout = CobaltLuaMachine.this.timeout;
        }

        @Override
        public void onInstruction( DebugState ds, DebugFrame di, int pc ) throws LuaError, UnwindThrowable
        {
            di.pc = pc;

            if( this.isPaused )
            {
                this.resetPaused( ds, di );
            }

            // We check our current pause/abort state every 128 instructions.
            if( (this.count = (this.count + 1) & 127) == 0 )
            {
                // If we've been hard aborted or closed then abort.
                if( this.timeout.isHardAborted() || CobaltLuaMachine.this.m_state == null )
                {
                    throw HardAbortError.INSTANCE;
                }

                this.timeout.refresh();
                if( this.timeout.isPaused() )
                {
                    // Preserve the current state
                    this.isPaused = true;
                    this.oldInHook = ds.inhook;
                    this.oldFlags = di.flags;

                    // Suspend the state. This will probably throw, but we need to handle the case where it won't.
                    di.flags |= FLAG_HOOKYIELD | FLAG_HOOKED;
                    LuaThread.suspend( ds.getLuaState() );
                    this.resetPaused( ds, di );
                }

                this.handleSoftAbort();
            }

            super.onInstruction( ds, di, pc );
        }

        @Override
        public void poll() throws LuaError
        {
            // If we've been hard aborted or closed then abort.
            LuaState state = CobaltLuaMachine.this.m_state;
            if( this.timeout.isHardAborted() || state == null )
            {
                throw HardAbortError.INSTANCE;
            }

            this.timeout.refresh();
            if( this.timeout.isPaused() )
            {
                LuaThread.suspendBlocking( state );
            }
            this.handleSoftAbort();
        }

        private void resetPaused( DebugState ds, DebugFrame di )
        {
            // Restore the previous paused state
            this.isPaused = false;
            ds.inhook = this.oldInHook;
            di.flags = this.oldFlags;
        }

        private void handleSoftAbort() throws LuaError
        {
            // If we already thrown our soft abort error then don't do it again.
            if( !this.timeout.isSoftAborted() || this.thrownSoftAbort )
            {
                return;
            }

            this.thrownSoftAbort = true;
            throw new LuaError( TimeoutState.ABORT_MESSAGE );
        }
    }

    private class CobaltLuaContext implements ILuaContext
    {
        @Override
        public long issueMainThreadTask( @Nonnull final ILuaTask task ) throws LuaException
        {
            // Issue command
            final long taskID = MainThread.getUniqueTaskID();
            final Runnable iTask = () -> {
                try
                {
                    Object[] results = task.execute();
                    if( results != null )
                    {
                        Object[] eventArguments = new Object[results.length + 2];
                        eventArguments[0] = taskID;
                        eventArguments[1] = true;
                        System.arraycopy( results, 0, eventArguments, 2, results.length );
                        CobaltLuaMachine.this.m_computer.queueEvent( "task_complete", eventArguments );
                    }
                    else
                    {
                        CobaltLuaMachine.this.m_computer.queueEvent( "task_complete",
                            new Object[] {
                                taskID,
                                true
                            } );
                    }
                }
                catch( LuaException e )
                {
                    CobaltLuaMachine.this.m_computer.queueEvent( "task_complete",
                        new Object[] {
                            taskID,
                            false,
                            e.getMessage()
                        } );
                }
                catch( Throwable t )
                {
                    if( ComputerCraft.logComputerErrors )
                    {
                        ComputerCraft.log.error( "Error running task", t );
                    }
                    CobaltLuaMachine.this.m_computer.queueEvent( "task_complete", new Object[] {
                        taskID,
                        false,
                        "Java Exception Thrown: " + t,
                    } );
                }
            };
            if( CobaltLuaMachine.this.m_computer.queueMainThread( iTask ) )
            {
                return taskID;
            }
            else
            {
                throw new LuaException( "Task limit exceeded" );
            }
        }
    }
}
