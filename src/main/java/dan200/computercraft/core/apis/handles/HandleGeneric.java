package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.ICallContext;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;

public abstract class HandleGeneric implements ILuaObject
{
    protected final Closeable m_closable;
    protected boolean m_open = true;

    public HandleGeneric( Closeable m_closable )
    {
        this.m_closable = m_closable;
    }

    protected void checkOpen() throws LuaException
    {
        if( !m_open ) throw new LuaException( "attempt to use a closed file" );
    }

    protected void close()
    {
        try
        {
            m_closable.close();
            m_open = false;
        }
        catch( IOException ignored )
        {
        }
    }

    @Nullable
    @Override
    @Deprecated
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        return callMethod( (ICallContext) context, method, arguments ).evaluate( context );
    }
}
