/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ITask;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.util.ThreadUtils;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;
import org.squiddev.cobalt.lib.platform.VoidResourceManipulator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

public class CobaltLuaMachine implements ILuaMachine
{
    private static final ThreadPoolExecutor coroutines = new ThreadPoolExecutor(
        0, Integer.MAX_VALUE,
        5L, TimeUnit.MINUTES,
        new SynchronousQueue<>(),
        ThreadUtils.factory( "Coroutine" )
    );

    private final Computer m_computer;

    private LuaState m_state;
    private LuaTable m_globals;
    private LuaThread m_mainRoutine;

    private String m_eventFilter;
    private String m_softAbortMessage;
    private String m_hardAbortMessage;

    public CobaltLuaMachine( Computer computer )
    {
        m_computer = computer;

        // Create an environment to run in
        LuaState state = this.m_state = LuaState.builder()
            .resourceManipulator( new VoidResourceManipulator() )
            .debug( new DebugHandler()
            {
                private int count = 0;
                private boolean hasSoftAbort;

                @Override
                public void onInstruction( DebugState ds, DebugFrame di, int pc ) throws LuaError, UnwindThrowable
                {
                    int count = ++this.count;
                    if( count > 100000 )
                    {
                        if( m_hardAbortMessage != null ) throw HardAbortError.INSTANCE;
                        this.count = 0;
                    }
                    else
                    {
                        handleSoftAbort();
                    }

                    super.onInstruction( ds, di, pc );
                }

                @Override
                public void poll() throws LuaError
                {
                    if( m_hardAbortMessage != null ) throw HardAbortError.INSTANCE;
                    handleSoftAbort();
                }

                private void handleSoftAbort() throws LuaError
                {
                    // If the soft abort has been cleared then we can reset our flags and continue.
                    String message = m_softAbortMessage;
                    if( message == null )
                    {
                        hasSoftAbort = false;
                        return;
                    }

                    if( hasSoftAbort && m_hardAbortMessage == null )
                    {
                        // If we have fired our soft abort, but we haven't been hard aborted then everything is OK.
                        return;
                    }

                    hasSoftAbort = true;
                    throw new LuaError( message );
                }
            } )
            .yieldThreader( command -> {
                Tracking.addValue( m_computer, TrackingField.COROUTINES_CREATED, 1 );
                coroutines.execute( () -> {
                    try
                    {
                        command.run();
                    }
                    finally
                    {
                        Tracking.addValue( m_computer, TrackingField.COROUTINES_DISPOSED, 1 );
                    }
                } );
            } )
            .build();

        m_globals = new LuaTable();
        state.setupThread( m_globals );

        // Add basic libraries
        m_globals.load( state, new BaseLib() );
        m_globals.load( state, new TableLib() );
        m_globals.load( state, new StringLib() );
        m_globals.load( state, new MathLib() );
        m_globals.load( state, new CoroutineLib() );
        m_globals.load( state, new Bit32Lib() );
        if( ComputerCraft.debug_enable ) m_globals.load( state, new DebugLib() );

        // Register custom load/loadstring provider which automatically adds prefixes.
        LibFunction.bind( m_globals, PrefixLoader.class, new String[] { "load", "loadstring" } );

        // Remove globals we don't want to expose
        m_globals.rawset( "collectgarbage", Constants.NIL );
        m_globals.rawset( "dofile", Constants.NIL );
        m_globals.rawset( "loadfile", Constants.NIL );
        m_globals.rawset( "print", Constants.NIL );

        // Add version globals
        m_globals.rawset( "_VERSION", valueOf( "Lua 5.1" ) );
        m_globals.rawset( "_HOST", valueOf( computer.getAPIEnvironment().getComputerEnvironment().getHostString() ) );
        m_globals.rawset( "_CC_DEFAULT_SETTINGS", valueOf( ComputerCraft.default_computer_settings ) );
        if( ComputerCraft.disable_lua51_features )
        {
            m_globals.rawset( "_CC_DISABLE_LUA51_FEATURES", Constants.TRUE );
        }

        // Our main function will go here
        m_mainRoutine = null;
        m_eventFilter = null;

        m_softAbortMessage = null;
        m_hardAbortMessage = null;
    }

    @Override
    public void addAPI( ILuaAPI api )
    {
        // Add the methods of an API to the global table
        LuaTable table = wrapLuaObject( api );
        String[] names = api.getNames();
        for( String name : names )
        {
            m_globals.rawset( name, table );
        }
    }

    @Override
    public void loadBios( InputStream bios )
    {
        // Begin executing a file (ie, the bios)
        if( m_mainRoutine != null ) return;

        try
        {
            LuaFunction value = LoadState.load( m_state, bios, "@bios.lua", m_globals );
            m_mainRoutine = new LuaThread( m_state, value, m_globals );
        }
        catch( CompileException e )
        {
            unload();
        }
        catch( IOException e )
        {
            ComputerCraft.log.warn( "Could not load bios.lua ", e );
            unload();
        }
    }

    @Override
    public void handleEvent( String eventName, Object[] arguments )
    {
        if( m_mainRoutine == null ) return;

        if( m_eventFilter != null && eventName != null && !eventName.equals( m_eventFilter ) && !eventName.equals( "terminate" ) )
        {
            return;
        }

        try
        {
            Varargs resumeArgs = Constants.NONE;
            if( eventName != null )
            {
                resumeArgs = varargsOf( valueOf( eventName ), toValues( arguments ) );
            }

            Varargs results = LuaThread.run( m_mainRoutine, resumeArgs );
            if( m_hardAbortMessage != null ) throw new LuaError( m_hardAbortMessage );

            LuaValue filter = results.first();
            m_eventFilter = filter.isString() ? filter.toString() : null;

            if( m_mainRoutine.getStatus().equals( "dead" ) ) unload();
        }
        catch( LuaError | HardAbortError e )
        {
            unload();
            ComputerCraft.log.warn( "Top level coroutine errored", e );
        }
        finally
        {
            m_softAbortMessage = null;
            m_hardAbortMessage = null;
        }
    }

    @Override
    public void softAbort( String abortMessage )
    {
        m_softAbortMessage = abortMessage;
    }

    @Override
    public void hardAbort( String abortMessage )
    {
        m_softAbortMessage = abortMessage;
        m_hardAbortMessage = abortMessage;
    }

    @Override
    public boolean saveState( OutputStream output )
    {
        return false;
    }

    @Override
    public boolean restoreState( InputStream input )
    {
        return false;
    }

    @Override
    public boolean isFinished()
    {
        return m_mainRoutine == null;
    }

    @Override
    public void unload()
    {
        if( m_state == null ) return;

        m_state.abandon();
        m_mainRoutine = null;
        m_state = null;
        m_globals = null;
    }

    private LuaTable wrapLuaObject( ILuaObject object )
    {
        LuaTable table = new LuaTable();
        String[] methods = object.getMethodNames();
        for( int i = 0; i < methods.length; i++ )
        {
            if( methods[i] != null )
            {
                final int method = i;
                final ILuaObject apiObject = object;
                final String methodName = methods[i];
                table.rawset( methodName, new VarArgFunction()
                {
                    @Override
                    public Varargs invoke( final LuaState state, Varargs _args ) throws LuaError
                    {
                        Object[] arguments = toObjects( _args, 1 );
                        Object[] results;
                        try
                        {
                            results = apiObject.callMethod( new ILuaContext()
                            {
                                @Nonnull
                                @Override
                                public Object[] pullEvent( String filter ) throws LuaException, InterruptedException
                                {
                                    Object[] results = pullEventRaw( filter );
                                    if( results.length >= 1 && results[0].equals( "terminate" ) )
                                    {
                                        throw new LuaException( "Terminated", 0 );
                                    }
                                    return results;
                                }

                                @Nonnull
                                @Override
                                public Object[] pullEventRaw( String filter ) throws InterruptedException
                                {
                                    return yield( new Object[] { filter } );
                                }

                                @Nonnull
                                @Override
                                public Object[] yield( Object[] yieldArgs ) throws InterruptedException
                                {
                                    try
                                    {
                                        Varargs results = LuaThread.yieldBlocking( state, toValues( yieldArgs ) );
                                        return toObjects( results, 1 );
                                    }
                                    catch( LuaError e )
                                    {
                                        throw new IllegalStateException( e );
                                    }
                                }

                                @Override
                                public long issueMainThreadTask( @Nonnull final ILuaTask task ) throws LuaException
                                {
                                    // Issue command
                                    final long taskID = MainThread.getUniqueTaskID();
                                    final ITask iTask = new ITask()
                                    {
                                        @Override
                                        public Computer getOwner()
                                        {
                                            return m_computer;
                                        }

                                        @Override
                                        public void execute()
                                        {
                                            try
                                            {
                                                Object[] results = task.execute();
                                                if( results != null )
                                                {
                                                    Object[] eventArguments = new Object[results.length + 2];
                                                    eventArguments[0] = taskID;
                                                    eventArguments[1] = true;
                                                    System.arraycopy( results, 0, eventArguments, 2, results.length );
                                                    m_computer.queueEvent( "task_complete", eventArguments );
                                                }
                                                else
                                                {
                                                    m_computer.queueEvent( "task_complete", new Object[] { taskID, true } );
                                                }
                                            }
                                            catch( LuaException e )
                                            {
                                                m_computer.queueEvent( "task_complete", new Object[] {
                                                    taskID, false, e.getMessage()
                                                } );
                                            }
                                            catch( Throwable t )
                                            {
                                                if( ComputerCraft.logPeripheralErrors )
                                                {
                                                    ComputerCraft.log.error( "Error running task", t );
                                                }
                                                m_computer.queueEvent( "task_complete", new Object[] {
                                                    taskID, false, "Java Exception Thrown: " + t.toString()
                                                } );
                                            }
                                        }
                                    };
                                    if( MainThread.queueTask( iTask ) )
                                    {
                                        return taskID;
                                    }
                                    else
                                    {
                                        throw new LuaException( "Task limit exceeded" );
                                    }
                                }

                                @Override
                                public Object[] executeMainThreadTask( @Nonnull final ILuaTask task ) throws LuaException, InterruptedException
                                {
                                    // Issue task
                                    final long taskID = issueMainThreadTask( task );

                                    // Wait for response
                                    while( true )
                                    {
                                        Object[] response = pullEvent( "task_complete" );
                                        if( response.length >= 3 && response[1] instanceof Number && response[2] instanceof Boolean )
                                        {
                                            if( ((Number) response[1]).intValue() == taskID )
                                            {
                                                Object[] returnValues = new Object[response.length - 3];
                                                if( (Boolean) response[2] )
                                                {
                                                    // Extract the return values from the event and return them
                                                    System.arraycopy( response, 3, returnValues, 0, returnValues.length );
                                                    return returnValues;
                                                }
                                                else
                                                {
                                                    // Extract the error message from the event and raise it
                                                    if( response.length >= 4 && response[3] instanceof String )
                                                    {
                                                        throw new LuaException( (String) response[3] );
                                                    }
                                                    else
                                                    {
                                                        throw new LuaException();
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }
                            }, method, arguments );
                        }
                        catch( InterruptedException e )
                        {
                            throw new OrphanedThread();
                        }
                        catch( LuaException e )
                        {
                            throw new LuaError( e.getMessage(), e.getLevel() );
                        }
                        catch( Throwable t )
                        {
                            if( ComputerCraft.logPeripheralErrors )
                            {
                                ComputerCraft.log.error( "Error calling " + methodName + " on " + apiObject, t );
                            }
                            throw new LuaError( "Java Exception Thrown: " + t.toString(), 0 );
                        }
                        return toValues( results );
                    }
                } );
            }
        }
        return table;
    }

    private LuaValue toValue( Object object, Map<Object, LuaValue> values )
    {
        if( object == null )
        {
            return Constants.NIL;
        }
        else if( object instanceof Number )
        {
            double d = ((Number) object).doubleValue();
            return valueOf( d );
        }
        else if( object instanceof Boolean )
        {
            return valueOf( (Boolean) object );
        }
        else if( object instanceof String )
        {
            String s = object.toString();
            return valueOf( s );
        }
        else if( object instanceof byte[] )
        {
            byte[] b = (byte[]) object;
            return valueOf( Arrays.copyOf( b, b.length ) );
        }
        else if( object instanceof Map )
        {
            // Table:
            // Start remembering stuff
            if( values == null )
            {
                values = new IdentityHashMap<>();
            }
            else if( values.containsKey( object ) )
            {
                return values.get( object );
            }
            LuaTable table = new LuaTable();
            values.put( object, table );

            // Convert all keys
            for( Map.Entry<?, ?> pair : ((Map<?, ?>) object).entrySet() )
            {
                LuaValue key = toValue( pair.getKey(), values );
                LuaValue value = toValue( pair.getValue(), values );
                if( !key.isNil() && !value.isNil() )
                {
                    table.rawset( key, value );
                }
            }
            return table;
        }
        else if( object instanceof ILuaObject )
        {
            return wrapLuaObject( (ILuaObject) object );
        }
        else
        {
            return Constants.NIL;
        }
    }

    private Varargs toValues( Object[] objects )
    {
        if( objects == null || objects.length == 0 )
        {
            return Constants.NONE;
        }

        LuaValue[] values = new LuaValue[objects.length];
        for( int i = 0; i < values.length; i++ )
        {
            Object object = objects[i];
            values[i] = toValue( object, null );
        }
        return varargsOf( values );
    }

    private static Object toObject( LuaValue value, Map<LuaValue, Object> objects )
    {
        switch( value.type() )
        {
            case Constants.TNIL:
            case Constants.TNONE:
            {
                return null;
            }
            case Constants.TINT:
            case Constants.TNUMBER:
            {
                return value.toDouble();
            }
            case Constants.TBOOLEAN:
            {
                return value.toBoolean();
            }
            case Constants.TSTRING:
            {
                return value.toString();
            }
            case Constants.TTABLE:
            {
                // Table:
                // Start remembering stuff
                if( objects == null )
                {
                    objects = new IdentityHashMap<>();
                }
                else if( objects.containsKey( value ) )
                {
                    return objects.get( value );
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
            {
                return null;
            }
        }
    }

    private static Object[] toObjects( Varargs values, int startIdx )
    {
        int count = values.count();
        Object[] objects = new Object[count - startIdx + 1];
        for( int n = startIdx; n <= count; n++ )
        {
            int i = n - startIdx;
            LuaValue value = values.arg( n );
            objects[i] = toObject( value, null );
        }
        return objects;
    }

    private static class PrefixLoader extends VarArgFunction
    {
        private static final LuaString FUNCTION_STR = valueOf( "function" );
        private static final LuaString EQ_STR = valueOf( "=" );

        @Override
        public Varargs invoke( LuaState state, Varargs args ) throws LuaError
        {
            switch( opcode )
            {
                case 0: // "load", // ( func [,chunkname] ) -> chunk | nil, msg
                {
                    LuaValue func = args.arg( 1 ).checkFunction();
                    LuaString chunkname = args.arg( 2 ).optLuaString( FUNCTION_STR );
                    if( !chunkname.startsWith( '@' ) && !chunkname.startsWith( '=' ) )
                    {
                        chunkname = OperationHelper.concat( EQ_STR, chunkname );
                    }
                    return BaseLib.loadStream( state, new StringInputStream( state, func ), chunkname );
                }
                case 1: // "loadstring", // ( string [,chunkname] ) -> chunk | nil, msg
                {
                    LuaString script = args.arg( 1 ).checkLuaString();
                    LuaString chunkname = args.arg( 2 ).optLuaString( script );
                    if( !chunkname.startsWith( '@' ) && !chunkname.startsWith( '=' ) )
                    {
                        chunkname = OperationHelper.concat( EQ_STR, chunkname );
                    }
                    return BaseLib.loadStream( state, script.toInputStream(), chunkname );
                }
            }

            return NONE;
        }
    }

    private static class StringInputStream extends InputStream
    {
        private final LuaState state;
        private final LuaValue func;
        private byte[] bytes;
        private int offset, remaining = 0;

        StringInputStream( LuaState state, LuaValue func )
        {
            this.state = state;
            this.func = func;
        }

        @Override
        public int read() throws IOException
        {
            if( remaining <= 0 )
            {
                LuaValue s;
                try
                {
                    s = OperationHelper.noYield( state, () -> OperationHelper.call( state, func ) );
                }
                catch( LuaError e )
                {
                    throw new IOException( e );
                }

                if( s.isNil() )
                {
                    return -1;
                }
                LuaString ls;
                try
                {
                    ls = s.strvalue();
                }
                catch( LuaError e )
                {
                    throw new IOException( e );
                }
                bytes = ls.bytes;
                offset = ls.offset;
                remaining = ls.length;
                if( remaining <= 0 )
                {
                    return -1;
                }
            }
            --remaining;
            return bytes[offset++];
        }
    }

    private static class HardAbortError extends Error
    {
        private static final long serialVersionUID = 7954092008586367501L;

        public static final HardAbortError INSTANCE = new HardAbortError();

        private HardAbortError()
        {
            super( "Hard Abort", null, true, false );
        }
    }
}
