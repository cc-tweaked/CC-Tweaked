/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.ILuaAPI;

import java.io.InputStream;

public interface ILuaMachine
{
    void addAPI( ILuaAPI api );

    void loadBios( InputStream bios );

    void handleEvent( String eventName, Object[] arguments );

    boolean isFinished();

    void close();
}
