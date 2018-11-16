/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ObjectWrapper implements ILuaContext
{
    private final ILuaObject object;
    private final String[] methods;

    public ObjectWrapper( ILuaObject object )
    {
        this.object = object;
        this.methods = object.getMethodNames();
    }

    private int findMethod( String method )
    {
        for( int i = 0; i < methods.length; i++ )
        {
            if( method.equals( methods[i] ) ) return i;
        }
        return -1;
    }

    public boolean hasMethod( String method )
    {
        return findMethod( method ) >= 0;
    }

    public Object[] call( String name, Object... args ) throws LuaException
    {
        int method = findMethod( name );
        if( method < 0 ) throw new IllegalStateException( "No such method '" + name + "'" );

        try
        {
            return object.callMethod( this, method, args );
        }
        catch( InterruptedException e )
        {
            throw new IllegalStateException( "Should never be interrupted", e );
        }
    }

    @Nonnull
    @Override
    public Object[] pullEvent( @Nullable String filter )
    {
        throw new IllegalStateException( "Method should never yield" );
    }

    @Nonnull
    @Override
    public Object[] pullEventRaw( @Nullable String filter )
    {
        throw new IllegalStateException( "Method should never yield" );
    }

    @Nonnull
    @Override
    public Object[] yield( @Nullable Object[] arguments )
    {
        throw new IllegalStateException( "Method should never yield" );
    }

    @Nullable
    @Override
    public Object[] executeMainThreadTask( @Nonnull ILuaTask task )
    {
        throw new IllegalStateException( "Method should never yield" );
    }

    @Override
    public long issueMainThreadTask( @Nonnull ILuaTask task )
    {
        throw new IllegalStateException( "Method should never queue events" );
    }
}
