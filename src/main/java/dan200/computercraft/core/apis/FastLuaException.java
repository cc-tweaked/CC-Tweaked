/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nullable;

/**
 * A Lua exception which does not contain its stack trace.
 */
public class FastLuaException extends LuaException
{
    private static final long serialVersionUID = 5957864899303561143L;

    public FastLuaException( @Nullable String message )
    {
        super( message );
    }

    public FastLuaException( @Nullable String message, int level )
    {
        super( message, level );
    }

    @Override
    public synchronized Throwable fillInStackTrace()
    {
        return this;
    }
}
