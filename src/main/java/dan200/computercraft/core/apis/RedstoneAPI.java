/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.computer.ComputerSide;

import javax.annotation.Nonnull;

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

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "getSides",
            "setOutput",
            "getOutput",
            "getInput",
            "setBundledOutput",
            "getBundledOutput",
            "getBundledInput",
            "testBundledInput",
            "setAnalogOutput",
            "setAnalogueOutput",
            "getAnalogOutput",
            "getAnalogueOutput",
            "getAnalogInput",
            "getAnalogueInput",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0: // getSides
                return new Object[] { ComputerSide.NAMES };
            case 1:
            {
                // setOutput
                ComputerSide side = parseSide( args );
                boolean output = getBoolean( args, 1 );
                environment.setOutput( side, output ? 15 : 0 );
                return null;
            }
            case 2: // getOutput
                return new Object[] { environment.getOutput( parseSide( args ) ) > 0 };
            case 3: // getInput
                return new Object[] { environment.getInput( parseSide( args ) ) > 0 };
            case 4:
            {
                // setBundledOutput
                ComputerSide side = parseSide( args );
                int output = getInt( args, 1 );
                environment.setBundledOutput( side, output );
                return null;
            }
            case 5: // getBundledOutput
                return new Object[] { environment.getBundledOutput( parseSide( args ) ) };
            case 6: // getBundledInput
                return new Object[] { environment.getBundledInput( parseSide( args ) ) };
            case 7:
            {
                // testBundledInput
                ComputerSide side = parseSide( args );
                int mask = getInt( args, 1 );
                int input = environment.getBundledInput( side );
                return new Object[] { (input & mask) == mask };
            }
            case 8:
            case 9:
            {
                // setAnalogOutput/setAnalogueOutput
                ComputerSide side = parseSide( args );
                int output = getInt( args, 1 );
                if( output < 0 || output > 15 )
                {
                    throw new LuaException( "Expected number in range 0-15" );
                }
                environment.setOutput( side, output );
                return null;
            }
            case 10:
            case 11: // getAnalogOutput/getAnalogueOutput
                return new Object[] { environment.getOutput( parseSide( args ) ) };
            case 12:
            case 13: // getAnalogInput/getAnalogueInput
                return new Object[] { environment.getInput( parseSide( args ) ) };
            default:
                return null;
        }
    }

    private static ComputerSide parseSide( Object[] args ) throws LuaException
    {
        ComputerSide side = ComputerSide.valueOfInsensitive( getString( args, 0 ) );
        if( side == null ) throw new LuaException( "Invalid side." );
        return side;
    }
}
