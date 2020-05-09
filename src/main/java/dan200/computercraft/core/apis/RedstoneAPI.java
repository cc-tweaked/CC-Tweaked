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

import static dan200.computercraft.api.lua.ArgumentHelper.*;

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
    public final void setOutput( Object[] args ) throws LuaException
    {
        ComputerSide side = parseSide( args );
        boolean output = getBoolean( args, 1 );
        environment.setOutput( side, output ? 15 : 0 );
    }

    @LuaFunction
    public final boolean getOutput( Object[] args ) throws LuaException
    {
        return environment.getOutput( parseSide( args ) ) > 0;
    }

    @LuaFunction
    public final boolean getInput( Object[] args ) throws LuaException
    {
        return environment.getInput( parseSide( args ) ) > 0;
    }

    @LuaFunction( { "setAnalogOutput", "setAnalogueOutput" } )
    public final void setAnalogOutput( Object[] args ) throws LuaException
    {
        ComputerSide side = parseSide( args );
        int output = getInt( args, 1 );
        if( output < 0 || output > 15 ) throw new LuaException( "Expected number in range 0-15" );
        environment.setOutput( side, output );
    }

    @LuaFunction( { "getAnalogOutput", "getAnalogueOutput" } )
    public final int getAnalogOutput( Object[] args ) throws LuaException
    {
        return environment.getOutput( parseSide( args ) );
    }

    @LuaFunction( { "getAnalogInput", "getAnalogueInput" } )
    public final int getAnalogInput( Object[] args ) throws LuaException
    {
        return environment.getInput( parseSide( args ) );
    }

    @LuaFunction
    public final void setBundledOutput( Object[] args ) throws LuaException
    {
        ComputerSide side = parseSide( args );
        int output = getInt( args, 1 );
        environment.setBundledOutput( side, output );
    }

    @LuaFunction
    public final int getBundledOutput( Object[] args ) throws LuaException
    {
        return environment.getBundledOutput( parseSide( args ) );
    }

    @LuaFunction
    public final int getBundledInput( Object[] args ) throws LuaException
    {
        return environment.getBundledOutput( parseSide( args ) );
    }

    @LuaFunction
    public final boolean testBundledInput( Object[] args ) throws LuaException
    {
        ComputerSide side = parseSide( args );
        int mask = getInt( args, 1 );
        int input = environment.getBundledInput( side );
        return (input & mask) == mask;
    }

    private static ComputerSide parseSide( Object[] args ) throws LuaException
    {
        ComputerSide side = ComputerSide.valueOfInsensitive( getString( args, 0 ) );
        if( side == null ) throw new LuaException( "Invalid side." );
        return side;
    }
}
