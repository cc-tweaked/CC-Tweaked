/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

/**
 * This exists purely to ensure binary compatibility.
 *
 * @see dan200.computercraft.api.lua.ILuaAPI
 */
@Deprecated
public interface ILuaAPI extends dan200.computercraft.api.lua.ILuaAPI
{
    void advance( double v );

    @Override
    default void update()
    {
        advance( 0.05 );
    }
}
