/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.terminal.TextBuffer;

import javax.annotation.Nonnull;

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

        @Override
        public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
        {
            switch( method )
            {
                case 0:
                {
                    // len
                    return new Object[] { m_buffer.length() };
                }
                case 1:
                {
                    // tostring
                    return new Object[] { m_buffer.toString() };
                }
                case 2:
                {
                    // read
                    int start = optInt( arguments, 0, 0 );
                    int end = optInt( arguments, 1, m_buffer.length() );
                    return new Object[] { m_buffer.read( start, end ) };
                }
                case 3:
                {
                    // write
                    String text = getString( arguments, 0 );
                    int start = optInt( arguments, 1, 0 );
                    int end = optInt( arguments, 2, start + text.length() );
                    m_buffer.write( text, start, end );
                    return null;
                }
                case 4:
                {
                    // fill
                    String text = getString( arguments, 0 );
                    int start = optInt( arguments, 1, 0 );
                    int end = optInt( arguments, 2, m_buffer.length() );
                    m_buffer.fill( text, start, end );
                    return null;
                }
                default:
                {
                    return null;
                }
            }
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

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
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
                return new Object[] { new BufferLuaObject( buffer ) };
            }
            default:
            {
                return null;
            }
        }
    }
}
