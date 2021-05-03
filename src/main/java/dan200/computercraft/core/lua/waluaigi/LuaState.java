package dan200.computercraft.core.lua.waluaigi;

import cc.tweaked.waluaigi.*;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.asm.LuaMethod;
import dan200.computercraft.core.asm.ObjectSource;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.lua.LuaContext;
import dan200.computercraft.core.lua.MachineResult;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

class LuaState implements ExecutionEnvironment
{
    private static final int INITIAL_MEMORY = 1 * 1024 * 1024;
    private static final int STRING_BUFFER_SIZE = 1024;
    private static final IDynamicLuaObject[] EMPTY_OBJECTS = new IDynamicLuaObject[0];

    private final ILuaContext context;
    private final Lua lua;
    private final Memory memory;

    private final TimeoutState timeout;
    private int insnCount = 0;

    private IDynamicLuaObject[] objects = EMPTY_OBJECTS;
    private final IntPriorityQueue freeSlots = new IntArrayFIFOQueue( 0 );
    private int nextSlot = 0;

    private final int mainState;
    private final int childState;

    private final int intBox;
    private final int stringBuffer;

    private String eventFilter;

    public LuaState( int maxMemory, Computer computer, TimeoutState timeout )
    {
        this.context = new LuaContext( computer );
        this.timeout = timeout;
        this.memory = new Memory( INITIAL_MEMORY, maxMemory );
        this.lua = LuaFactory.create( this, memory );

        this.intBox = lua.malloc( 4 );
        this.stringBuffer = lua.malloc( STRING_BUFFER_SIZE );
        this.mainState = lua.waluaigi_new_state();
        this.childState = lua.lua_newthread( mainState );
    }

    public boolean isOpen()
    {
        return mainState != 0 && childState != 0 && intBox != 0 && stringBuffer != 0;
    }

    public int allocate( IDynamicLuaObject object )
    {
        if( !freeSlots.isEmpty() )
        {
            int slot = freeSlots.dequeueInt();
            objects[slot] = object;
            return slot;
        }

        if( nextSlot >= objects.length )
        {
            objects = objects == EMPTY_OBJECTS ? new IDynamicLuaObject[16] : Arrays.copyOf( objects, objects.length * 2 );
        }

        int slot = nextSlot++;
        objects[slot] = object;
        return slot;
    }

    @Override
    public void free( int object )
    {
        if( object < 0 || object >= objects.length || objects[object] == null ) return;
        objects[object] = null;
        freeSlots.enqueue( object );
    }

    @Override
    public int invoke( int luaState, int object, int method )
    {
        IDynamicLuaObject luaObject;
        if( object < 0 || object >= objects.length || (luaObject = objects[object]) == null )
        {
            pushString( luaState, "Invalid object" );
            lua.lua_error( luaState );
            throw new UnreachableException();
        }

        int nargs = lua.lua_gettop( luaState );
        Object[] args = new Object[nargs];
        for( int i = 0; i < nargs; i++ ) args[i] = fromLua( luaState, i + 1 );

        MethodResult result;
        try
        {
            result = luaObject.callMethod( context, method, new ObjectArguments( args ) );
        }
        catch( LuaException e )
        {
            lua.luaL_where( luaState, e.getLevel() );
            pushString( luaState, e.getMessage() );
            lua.lua_concat( luaState, 2 );
            lua.lua_error( luaState );
            throw new UnreachableException();
        }

        if( result.getCallback() != null ) throw new IllegalStateException( "NYI: Yields" );

        Object[] values = result.getResult();
        if( values == null ) return 0;

        for( Object value : values ) pushObject( luaState, value );
        return values.length;
    }

    private Object fromLua( int luaState, int slot )
    {
        int type = lua.lua_type( luaState, slot );
        switch( type )
        {
            case LuaConstants.LUA_TNIL:
                return null;
            case LuaConstants.LUA_TBOOLEAN:
                return lua.lua_toboolean( luaState, slot ) != 0;
            case LuaConstants.LUA_TLIGHTUSERDATA:
                return null;
            case LuaConstants.LUA_TNUMBER:
                return lua.lua_tonumberx( luaState, slot, 0 );
            case LuaConstants.LUA_TSTRING:
                return getString( luaState, slot );
            case LuaConstants.LUA_TTABLE:
                ComputerCraft.log.warn( "NYI: conversion from table" );
                // TODO: Conversion fom tables
                return null;
            case LuaConstants.LUA_TFUNCTION:
            case LuaConstants.LUA_TUSERDATA:
            case LuaConstants.LUA_TTHREAD:
            default:
                return null;
        }
    }

    public void addAPI( ILuaAPI api )
    {
        int mainState = childState;
        pushLuaObject( mainState, api, true );
        for( String name : api.getNames() )
        {
            lua.lua_pushvalue( mainState, -1 );
            setShortString( name ); // TODO: Support long strings.
            lua.lua_setglobal( mainState, stringBuffer );
        }

        lua.lua_settop( mainState, -2 );
    }

    public MachineResult handleEvent( @Nullable String eventName, @Nullable Object[] arguments )
    {
        ComputerCraft.log.warn( "Resuming computer {} vs {}", eventName, eventFilter );

        if( eventFilter != null && eventName != null && !eventName.equals( eventFilter ) && !eventName.equals( "terminate" ) )
        {
            return MachineResult.OK;
        }

        // If the soft abort has been cleared then we can reset our flag.
        timeout.refresh();

        int args = 0;
        if( eventName != null )
        {
            args++;
            pushString( childState, eventName );
        }
        if( arguments != null )
        {
            args += arguments.length;
            for( Object object : arguments ) pushObject( childState, object );
        }

        int result = lua.lua_resume( childState, 0, args, intBox );
        switch( result )
        {
            case LuaConstants.LUA_OK:
            {
                ComputerCraft.log.warn( "Coroutine finished for no reason!" );
                return MachineResult.GENERIC_ERROR;
            }
            case LuaConstants.LUA_YIELD:
            {
                int nargs = memory.getInt( intBox );
                eventFilter = nargs > 0 && lua.lua_type( childState, 1 ) == LuaConstants.LUA_TSTRING
                    ? getString( childState, 1 )
                    : null;
                lua.lua_settop( childState, -nargs - 1 );
                ComputerCraft.log.warn( "Computer yielded OK with {}", eventFilter );
                return MachineResult.OK;
            }
            default:
            {
                String error = getString( childState, -1 );
                ComputerCraft.log.warn( "Top level coroutine errored ({})", error );
                return MachineResult.error( error );
            }
        }
    }

    private boolean pushDynamicObject( int luaState, IDynamicLuaObject object )
    {
        lua.waluaigi_pushobject( luaState, allocate( object ) );

        String[] methods = Objects.requireNonNull( object.getMethodNames(), "Methods cannot be null" );
        for( int i = 0; i < methods.length; i++ )
        {
            pushString( luaState, methods[i] );
            lua.lua_pushvalue( luaState, -2 );
            lua.waluaigi_pushfunction( luaState, i );
            lua.lua_rawset( luaState, -4 );
        }

        lua.lua_settop( luaState, -2 );

        return methods.length > 0;
    }

    private void pushLuaObject( int luaState, Object object, boolean force )
    {
        boolean hasMethods = false;

        lua.lua_createtable( luaState, 0, 0 );

        if( object instanceof IDynamicLuaObject )
        {
            hasMethods = pushDynamicObject( luaState, (IDynamicLuaObject) object );
        }

        hasMethods |= pushDynamicObject( luaState, new WrappedLuaObject( object, LuaMethod.GENERATOR.getMethods( object.getClass() ) ) );
        if( object instanceof ObjectSource )
        {
            for( Object extra : ((ObjectSource) object).getExtra() )
            {
                hasMethods |= pushDynamicObject( luaState, new WrappedLuaObject( extra, LuaMethod.GENERATOR.getMethods( extra.getClass() ) ) );
            }
        }

        if( hasMethods ) return;

        ComputerCraft.log.warn( "Received unknown type '{}'.", object.getClass().getName() );
        if( !force )
        {
            lua.lua_settop( luaState, -2 );
            lua.lua_pushnil( luaState );
        }
    }

    private void pushObject( int luaState, Object object )
    {
        if( object == null )
        {
            lua.lua_pushnil( luaState );
        }
        else if( object instanceof Long || object instanceof Integer )
        {
            lua.lua_pushinteger( luaState, ((Number) object).longValue() );
        }
        else if( object instanceof Number )
        {
            lua.lua_pushnumber( luaState, ((Number) object).doubleValue() );
        }
        else if( object instanceof Boolean )
        {
            lua.lua_pushboolean( luaState, (Boolean) object ? 1 : 0 );
        }
        else if( object instanceof String )
        {
            pushString( luaState, (String) object );
        }
        else if( object instanceof byte[] )
        {
            byte[] b = (byte[]) object;
            pushString( luaState, b, 0, b.length );
        }
        else if( object instanceof ByteBuffer )
        {
            pushString( luaState, (ByteBuffer) object );
        }
        else if( object instanceof IDynamicLuaObject )
        {
            pushLuaObject( luaState, object, true );
        }
        else if( object instanceof Map )
        {
            Map<?, ?> map = (Map<?, ?>) object;
            lua.lua_createtable( luaState, 0, map.size() );

            for( Map.Entry<?, ?> pair : map.entrySet() )
            {
                pushObject( luaState, pair.getKey() );
                pushObject( luaState, pair.getValue() );
                lua.lua_rawset( luaState, -3 );
            }
        }
        else if( object instanceof Collection )
        {
            Collection<?> collection = (Collection<?>) object;
            lua.lua_createtable( luaState, collection.size(), 0 );

            int i = 0;
            for( Object obj : collection )
            {
                pushObject( luaState, obj );
                lua.lua_rawseti( luaState, -2, ++i );
            }
        }
        else if( object instanceof Object[] )
        {
            Object[] collection = (Object[]) object;
            lua.lua_createtable( luaState, collection.length, 0 );

            for( int i = 0; i < collection.length; i++ )
            {
                pushObject( luaState, collection[i] );
                lua.lua_rawseti( luaState, -2, i + 1 );
            }
        }
        else
        {
            pushLuaObject( luaState, object, false );
        }
    }

    @Override
    public TimeoutMode getTimeout()
    {
        if( timeout.isHardAborted() ) return TimeoutMode.HALT;

        if( (insnCount = (insnCount + 1) & 127) == 0 )
        {
            timeout.refresh();
            if( timeout.isSoftAborted() ) return TimeoutMode.ERROR;
            if( timeout.isPaused() ) return TimeoutMode.PAUSE;
        }

        return TimeoutMode.OK;
    }

    public MachineResult loadString( ByteBuffer contents, String name )
    {
        int contentsLen = contents.remaining();
        int contentsLoc = lua.malloc( contentsLen );
        memory.put( contentsLoc, contents );

        setShortString( name );

        int result = lua.luaL_loadbufferx( childState, contentsLoc, contentsLen, stringBuffer, 0 );
        lua.free( contentsLoc );

        return result == 0 ? MachineResult.OK : MachineResult.error( "Cannot parse bios.lua: " + getString( childState, 1 ) );
    }

    private void setShortString( String string )
    {
        int length = string.length();
        if( length >= STRING_BUFFER_SIZE ) throw new IllegalArgumentException( "String must be <1024 characters" );
        memory.putString( stringBuffer, string );
        memory.put( stringBuffer + string.length(), (byte) 0 );
    }

    public void pushString( int luaState, String string )
    {
        int length = string.length();
        if( length <= STRING_BUFFER_SIZE )
        {
            memory.putString( stringBuffer, string );
            lua.lua_pushlstring( luaState, stringBuffer, length );
        }
        else
        {
            int position = lua.malloc( length + 1 );
            if( position == 0 ) throw new OutOfMemoryException( "Cannot allocate string" );

            memory.putString( position, string );
            lua.lua_pushlstring( luaState, position, length );
            lua.free( position );
        }
    }

    public void pushString( int luaState, ByteBuffer buffer )
    {
        int length = buffer.remaining();
        if( length <= STRING_BUFFER_SIZE )
        {
            memory.put( stringBuffer, buffer );
            lua.lua_pushlstring( luaState, stringBuffer, length );
        }
        else
        {
            int position = lua.malloc( length );
            if( position == 0 ) throw new OutOfMemoryException( "Cannot allocate string" );

            memory.put( position, buffer );
            lua.lua_pushlstring( luaState, position, length );
            lua.free( position );
        }
    }

    public void pushString( int luaState, byte[] bytes, int offset, int length )
    {
        if( length <= STRING_BUFFER_SIZE )
        {
            memory.put( stringBuffer, bytes, offset, length );
            lua.lua_pushlstring( luaState, stringBuffer, length );
        }
        else
        {
            int position = lua.malloc( length );
            if( position == 0 ) throw new OutOfMemoryException( "Cannot allocate string" );

            memory.put( position, bytes, offset, length );
            lua.lua_pushlstring( luaState, position, length );
            lua.free( position );
        }
    }

    public String getString( int luaState, int slot )
    {
        int stringPos = lua.luaL_checklstring( luaState, slot, intBox );
        if( stringPos == 0 ) throw new NullPointerException( "luaL_checklstring returned 0" );
        return memory.getString( stringPos, memory.getInt( intBox ) );
    }

    public ByteBuffer getStringAsBuffer( int luaState, int slot )
    {
        int stringPos = lua.luaL_checklstring( luaState, slot, intBox );
        if( stringPos == 0 ) throw new NullPointerException( "luaL_checklstring returned 0" );

        int length = memory.getInt( intBox );
        ByteBuffer buffer = ByteBuffer.allocate( length );
        memory.get( stringPos, buffer );
        buffer.flip();
        return buffer;
    }
}
