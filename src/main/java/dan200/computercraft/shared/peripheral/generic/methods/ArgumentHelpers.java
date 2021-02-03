/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.lua.LuaException;

/**
 * A few helpers for working with arguments.
 *
 * This should really be moved into the public API. However, until I have settled on a suitable format, we'll keep it
 * where it is used.
 */
final class ArgumentHelpers
{
    private ArgumentHelpers()
    {
    }

    public static void assertBetween( double value, double min, double max, String message ) throws LuaException
    {
        if( value < min || value > max || Double.isNaN( value ) )
        {
            throw new LuaException( String.format( message, "between " + min + " and " + max ) );
        }
    }

    public static void assertBetween( int value, int min, int max, String message ) throws LuaException
    {
        if( value < min || value > max )
        {
            throw new LuaException( String.format( message, "between " + min + " and " + max ) );
        }
    }
}
