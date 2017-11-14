/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ILuaAPI;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ITask;
import dan200.computercraft.core.computer.MainThread;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class LuaJLuaMachine implements ILuaMachine
{
    private Computer m_computer;

    private LuaValue m_globals;
    private LuaValue m_loadString;
    private LuaValue m_assert;
    private LuaValue m_coroutine_create;
    private LuaValue m_coroutine_resume;
    private LuaValue m_coroutine_yield;
    
    private LuaValue m_mainRoutine;
    private String m_eventFilter;
    private String m_softAbortMessage;
    private String m_hardAbortMessage;

    private Map<Object, LuaValue> m_valuesInProgress;
    private Map<LuaValue, Object> m_objectsInProgress;

    public LuaJLuaMachine( Computer computer )
    {
        m_computer = computer;

        // Create an environment to run in
        m_globals = JsePlatform.debugGlobals();
        m_globals.load( new Bit32Lib() );
        m_loadString = m_globals.get("loadstring");
        m_assert = m_globals.get("assert");

        LuaValue coroutine = m_globals.get("coroutine");
        final LuaValue native_coroutine_create = coroutine.get("create");
        
        LuaValue debug = m_globals.get("debug");
        final LuaValue debug_sethook = debug.get("sethook");
        
        coroutine.set("create", new OneArgFunction() {
            @Override
            public LuaValue call( LuaValue value )
            {
                final LuaThread thread = native_coroutine_create.call( value ).checkthread();
                debug_sethook.invoke( new LuaValue[] {
                    thread,
                    new ZeroArgFunction() {
                        @Override
                        public LuaValue call() {
                            String hardAbortMessage = m_hardAbortMessage;
                            if( hardAbortMessage != null )
                            {
                                LuaThread.yield(LuaValue.NIL);
                            }
                            return LuaValue.NIL;
                        }
                    },
                    LuaValue.NIL,
                    LuaValue.valueOf(100000)
                } );
                return thread;                
            }
        });
        
        m_coroutine_create = coroutine.get("create");
        m_coroutine_resume = coroutine.get("resume");
        m_coroutine_yield = coroutine.get("yield");
        
        // Remove globals we don't want to expose
        m_globals.set( "collectgarbage", LuaValue.NIL );
        m_globals.set( "dofile", LuaValue.NIL );
        m_globals.set( "loadfile", LuaValue.NIL );
        m_globals.set( "module", LuaValue.NIL );
        m_globals.set( "require", LuaValue.NIL );
        m_globals.set( "package", LuaValue.NIL );        
        m_globals.set( "io", LuaValue.NIL );
        m_globals.set( "os", LuaValue.NIL );
        m_globals.set( "print", LuaValue.NIL );
        m_globals.set( "luajava", LuaValue.NIL );
        m_globals.set( "debug", LuaValue.NIL );
        m_globals.set( "newproxy", LuaValue.NIL );
        m_globals.set( "__inext", LuaValue.NIL );

        // Add version globals
        m_globals.set( "_VERSION", "Lua 5.1" );
        m_globals.set( "_HOST", computer.getAPIEnvironment().getComputerEnvironment().getHostString() );
        m_globals.set( "_CC_DEFAULT_SETTINGS", toValue( ComputerCraft.default_computer_settings ) );
        if( ComputerCraft.disable_lua51_features )
        {
            m_globals.set( "_CC_DISABLE_LUA51_FEATURES", toValue( true ) );
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
            m_globals.set( name, table );
        }
    }
    
    @Override
    public void loadBios( InputStream bios )
    {
        // Begin executing a file (ie, the bios)
        if( m_mainRoutine != null )
        {
            return;
        }
        
        try
        {
            // Read the whole bios into a string
            String biosText;
            try
            {
                InputStreamReader isr;
                try
                {
                    isr = new InputStreamReader( bios, "UTF-8" );
                }
                catch( UnsupportedEncodingException e )
                {
                    isr = new InputStreamReader( bios );
                }
                BufferedReader reader = new BufferedReader( isr );
                StringBuilder fileText = new StringBuilder( "" );
                String line = reader.readLine();
                while( line != null ) {
                    fileText.append( line );
                    line = reader.readLine();
                    if( line != null ) {
                        fileText.append( "\n" );
                    }
                }
                biosText = fileText.toString();
            }
            catch( IOException e )
            {
                throw new LuaError( "Could not read file" );
            }
            
            // Load it
            LuaValue program = m_assert.call( m_loadString.call( 
                toValue( biosText ), toValue( "bios.lua" )
            ));
            m_mainRoutine = m_coroutine_create.call( program );
        }
        catch( LuaError e )
        {
            ComputerCraft.log.warn( "Could not load bios.lua ", e );
            if( m_mainRoutine != null )
            {
                ((LuaThread)m_mainRoutine).abandon();
                m_mainRoutine = null;
            }
        }
    }
    
    @Override
    public void handleEvent( String eventName, Object[] arguments )
    {
        if( m_mainRoutine == null )
        {
            return;
        }

        if( m_eventFilter != null && eventName != null && !eventName.equals( m_eventFilter ) && !eventName.equals( "terminate" ) )
        {
            return;
        }
        
        try
        {            
            LuaValue[] resumeArgs;
            if( eventName != null )
            {
                resumeArgs = toValues( arguments, 2 );
                resumeArgs[0] = m_mainRoutine;
                resumeArgs[1] = toValue( eventName );
            }
            else
            {
                resumeArgs = new LuaValue[1];
                resumeArgs[0] = m_mainRoutine;
            }
            
            Varargs results = m_coroutine_resume.invoke( LuaValue.varargsOf( resumeArgs ) );
            if( m_hardAbortMessage != null ) 
            {
                throw new LuaError( m_hardAbortMessage );
            }
            else if( results.arg1().checkboolean() == false )
            {
                throw new LuaError( results.arg(2).checkstring().toString() );
            }
            else
            {
                LuaValue filter = results.arg(2);
                if( filter.isstring() )
                {
                    m_eventFilter = filter.toString();
                }
                else
                {
                    m_eventFilter = null;
                }
            }
                        
            LuaThread mainThread = (LuaThread)m_mainRoutine;
            if( mainThread.getStatus().equals("dead") )
            {
                m_mainRoutine = null;
            }
        }
        catch( LuaError e )
        {
            ((LuaThread)m_mainRoutine).abandon();
            m_mainRoutine = null;
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
        return (m_mainRoutine == null);
    }
    
    @Override
    public void unload()
    {
        if( m_mainRoutine != null )
        {
            LuaThread mainThread = (LuaThread)m_mainRoutine;
            mainThread.abandon();
            m_mainRoutine = null;
        }
    }
        
    private void tryAbort() throws LuaError
    {
//        while( m_stopped )
//        {
//            m_coroutine_yield.call();
//        }
        
        String abortMessage = m_softAbortMessage;
        if( abortMessage != null )
        {
            m_softAbortMessage = null;
            m_hardAbortMessage = null;
            throw new LuaError( abortMessage );
        }
    }
    
    private LuaTable wrapLuaObject( ILuaObject object )
    {
        LuaTable table = new LuaTable();
        String[] methods = object.getMethodNames();
        for(int i=0; i<methods.length; ++i )
        {
            if( methods[i] != null )
            {
                final int method = i;
                final ILuaObject apiObject = object;
                final String methodName = methods[i];
                table.set( methodName, new VarArgFunction() {
                    @Override
                    public Varargs invoke( Varargs _args )
                    {
                        tryAbort();
                        Object[] arguments = toObjects( _args, 1 );
                        Object[] results;
                        try
                        {
                            results = apiObject.callMethod( new ILuaContext() {
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
                                        LuaValue[] yieldValues = toValues( yieldArgs, 0 );
                                        Varargs results = m_coroutine_yield.invoke( LuaValue.varargsOf( yieldValues ) );
                                        return toObjects( results, 1 );
                                    }
                                    catch( OrphanedThread e )
                                    {
                                        throw new InterruptedException();
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
                                                    Object[] eventArguments = new Object[ results.length + 2 ];
                                                    eventArguments[ 0 ] = taskID;
                                                    eventArguments[ 1 ] = true;
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
                                        if( response.length >= 3 && response[ 1 ] instanceof Number && response[ 2 ] instanceof Boolean )
                                        {
                                            if( ( (Number)response[ 1 ] ).intValue() == taskID )
                                            {
                                                Object[] returnValues = new Object[ response.length - 3 ];
                                                if( (Boolean)response[ 2 ] )
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
                                                        throw new LuaException( (String)response[ 3 ] );
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
                        return LuaValue.varargsOf( toValues( results, 0 ) );
                    }
                } );
            }
        }
        return table;
    }

    private LuaValue toValue( Object object )
    {
        if( object == null )
        {
            return LuaValue.NIL;
        }
        else if( object instanceof Number )
        {
            double d = ((Number)object).doubleValue();
            return LuaValue.valueOf( d );
        }
        else if( object instanceof Boolean )
        {
            boolean b = (Boolean) object;
            return LuaValue.valueOf( b );
        }
        else if( object instanceof String )
        {
            String s = object.toString();
            return LuaValue.valueOf( s );
        }
        else if( object instanceof byte[] )
        {
            byte[] b = (byte[]) object;
            return LuaValue.valueOf( Arrays.copyOf( b, b.length ) );
        }
        else if( object instanceof Map )
        {
            // Table:
            // Start remembering stuff
            boolean clearWhenDone = false;
            try
            {
                if( m_valuesInProgress == null )
                {
                    m_valuesInProgress = new IdentityHashMap<>();
                    clearWhenDone = true;
                }
                else if( m_valuesInProgress.containsKey( object ) )
                {
                    return m_valuesInProgress.get( object );
                }
                LuaValue table = new LuaTable();
                m_valuesInProgress.put( object, table );

                // Convert all keys
                for( Map.Entry<?, ?> pair : ((Map<?, ?>) object).entrySet() )
                {
                    LuaValue key = toValue( pair.getKey() );
                    LuaValue value = toValue( pair.getValue() );
                    if( !key.isnil() && !value.isnil() )
                    {
                        table.set( key, value );
                    }
                }
                return table;
            }
            finally
            {
                // Clear (if exiting top level)
                if( clearWhenDone )
                {
                    m_valuesInProgress = null;
                }
            }
        }
        else if( object instanceof ILuaObject )
        {
            return wrapLuaObject( (ILuaObject)object );
        }
        else
        {
            return LuaValue.NIL;
        }        
    }

    private LuaValue[] toValues( Object[] objects, int leaveEmpty )
    {
        if( objects == null || objects.length == 0 ) 
        {
            return new LuaValue[ leaveEmpty ];
        }
        
        LuaValue[] values = new LuaValue[objects.length + leaveEmpty];
        for( int i=0; i<values.length; ++i )
        {
            if( i < leaveEmpty )
            {
                values[i] = null;
                continue;
            }
            Object object = objects[i - leaveEmpty];
            values[i] = toValue( object );
        }
        return values;
    }

    private Object toObject( LuaValue value )
    {
        switch( value.type() )
        {
            case LuaValue.TNIL:
            case LuaValue.TNONE: 
            {
                return null;
            }
            case LuaValue.TINT:
            case LuaValue.TNUMBER:
            {
                return value.todouble();
            }
            case LuaValue.TBOOLEAN:
            {
                return value.toboolean();
            }
            case LuaValue.TSTRING:
            {
                LuaString str = value.checkstring();
                return str.tojstring();
            }
            case LuaValue.TTABLE:
            {
                // Table:
                boolean clearWhenDone = false;
                try
                {
                    // Start remembering stuff
                    if( m_objectsInProgress == null )
                    {
                        m_objectsInProgress = new IdentityHashMap<>();
                        clearWhenDone = true;
                    }
                    else if( m_objectsInProgress.containsKey( value ) )
                    {
                        return m_objectsInProgress.get( value );
                    }
                    Map<Object, Object> table = new HashMap<>();
                    m_objectsInProgress.put( value, table );

                    // Convert all keys
                    LuaValue k = LuaValue.NIL;
                    while( true )
                    {
                        Varargs keyValue = value.next( k );
                        k = keyValue.arg1();
                        if( k.isnil() )
                        {
                            break;
                        }

                        LuaValue v = keyValue.arg(2);
                        Object keyObject = toObject(k);
                        Object valueObject = toObject(v);
                        if( keyObject != null && valueObject != null )
                        {
                            table.put( keyObject, valueObject );
                        }
                    }
                    return table;
                }
                finally
                {
                    // Clear (if exiting top level)
                    if( clearWhenDone )
                    {
                        m_objectsInProgress = null;
                    }
                }
            }
            default:
            {
                return null;
            }
        }        
    }
    
    private Object[] toObjects( Varargs values, int startIdx )
    {
        int count = values.narg();
        Object[] objects = new Object[ count - startIdx + 1 ];
        for( int n=startIdx; n<=count; ++n )
        {
            int i = n - startIdx;
            LuaValue value = values.arg(n);
            objects[i] = toObject( value );
        }
        return objects;
    }
}
