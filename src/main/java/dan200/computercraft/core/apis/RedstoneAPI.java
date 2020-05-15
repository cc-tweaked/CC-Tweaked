/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.computer.ComputerSide;

public class RedstoneAPI implements ILuaAPI
{
    private final IAPIEnvironment environment;

    public RedstoneAPI( IAPIEnvironment environment )
    {
        this.environment = environment;
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "rs", "redstone" };
    }

    @LuaFunction
    public final String[] getSides()
    {
        return ComputerSide.NAMES;
    }

    @LuaFunction
    public final void setOutput( ComputerSide side, boolean output )
    {
        environment.setOutput( side, output ? 15 : 0 );
    }

    @LuaFunction
    public final boolean getOutput( ComputerSide side )
    {
        return environment.getOutput( side ) > 0;
    }

    @LuaFunction
    public final boolean getInput( ComputerSide side )
    {
        return environment.getInput( side ) > 0;
    }

    @LuaFunction( { "setAnalogOutput", "setAnalogueOutput" } )
    public final void setAnalogOutput( ComputerSide side, int output ) throws LuaException
    {
        if( output < 0 || output > 15 ) throw new LuaException( "Expected number in range 0-15" );
        environment.setOutput( side, output );
    }

    @LuaFunction( { "getAnalogOutput", "getAnalogueOutput" } )
    public final int getAnalogOutput( ComputerSide side )
    {
        return environment.getOutput( side );
    }

    @LuaFunction( { "getAnalogInput", "getAnalogueInput" } )
    public final int getAnalogInput( ComputerSide side )
    {
        return environment.getInput( side );
    }

    @LuaFunction
    public final void setBundledOutput( ComputerSide side, int output )
    {
        environment.setBundledOutput( side, output );
    }

    @LuaFunction
    public final int getBundledOutput( ComputerSide side )
    {
        return environment.getBundledOutput( side );
    }

    @LuaFunction
    public final int getBundledInput( ComputerSide side )
    {
        return environment.getBundledOutput( side );
    }

    @LuaFunction
    public final boolean testBundledInput( ComputerSide side, int mask )
    {
        int input = environment.getBundledInput( side );
        return (input & mask) == mask;
    }
}
