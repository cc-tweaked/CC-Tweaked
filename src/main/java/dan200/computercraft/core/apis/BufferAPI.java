/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.core.terminal.TextBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;
import static dan200.computercraft.core.apis.ArgumentHelper.optInt;

public class BufferAPI implements ILuaAPI
{
    private static class BufferLuaObject implements ILuaObject
    {
        private TextBuffer m_buffer;

        public BufferLuaObject( TextBuffer buffer )
        {
            m_buffer = buffer;
        }

        @Nonnull
        @Override
        public String[] getMethodNames()
        {
            return new String[] {
                "len",
                "tostring",
                "read",
                "write",
                "fill"
            };
        }

        @Nonnull
        @Override
        public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] arguments ) throws LuaException
        {
            switch( method )
            {
                case 0:
                {
                    // len
                    return MethodResult.of( m_buffer.length() );
                }
                case 1:
                {
                    // tostring
                    return MethodResult.of( m_buffer.toString() );
                }
                case 2:
                {
                    // read
                    int start = optInt( arguments, 0, 0 );
                    int end = optInt( arguments, 1, m_buffer.length() );
                    return MethodResult.of( m_buffer.read( start, end ) );
                }
                case 3:
                {
                    // write
                    String text = getString( arguments, 0 );
                    int start = optInt( arguments, 1, 0 );
                    int end = optInt( arguments, 2, start + text.length() );
                    m_buffer.write( text, start, end );
                    return MethodResult.empty();
                }
                case 4:
                {
                    // fill
                    String text = getString( arguments, 0 );
                    int start = optInt( arguments, 1, 0 );
                    int end = optInt( arguments, 2, m_buffer.length() );
                    m_buffer.fill( text, start, end );
                    return MethodResult.empty();
                }
                default:
                {
                    return MethodResult.empty();
                }
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

    public BufferAPI( IAPIEnvironment _env )
    {
    }

    @Override
    public String[] getNames()
    {
        return new String[] {
            "buffer"
        };
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "new"
        };
    }

    @Nonnull
    @Override
    public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] arguments ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                String text = getString( arguments, 0 );
                int repetitions = optInt( arguments, 1, 1 );
                if( repetitions < 0 )
                {
                    throw ArgumentHelper.badArgument( 1, "positive number", Integer.toString( repetitions ) );
                }
                TextBuffer buffer = new TextBuffer( text, repetitions );
                return MethodResult.of( new BufferLuaObject( buffer ) );
            }
            default:
            {
                return MethodResult.empty();
            }
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
