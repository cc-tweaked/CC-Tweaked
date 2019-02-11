/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static dan200.computercraft.core.apis.ArgumentHelper.*;

public class RedstoneAPI implements ILuaAPI
{
    private IAPIEnvironment m_environment;

    public RedstoneAPI( IAPIEnvironment environment )
    {
        m_environment = environment;
    }

    @Override
    public String[] getNames()
    {
        return new String[] {
            "rs", "redstone"
        };
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
            case 0:
            {
                // getSides
                Map<Object, Object> table = new HashMap<>();
                for( int i = 0; i < IAPIEnvironment.SIDE_NAMES.length; i++ )
                {
                    table.put( i + 1, IAPIEnvironment.SIDE_NAMES[i] );
                }
                return new Object[] { table };
            }
            case 1:
            {
                // setOutput
                int side = parseSide( args );
                boolean output = getBoolean( args, 1 );
                m_environment.setOutput( side, output ? 15 : 0 );
                return null;
            }
            case 2:
            {
                // getOutput
                int side = parseSide( args );
                return new Object[] { m_environment.getOutput( side ) > 0 };
            }
            case 3:
            {
                // getInput
                int side = parseSide( args );
                return new Object[] { m_environment.getInput( side ) > 0 };
            }
            case 4:
            {
                // setBundledOutput
                int side = parseSide( args );
                int output = getInt( args, 1 );
                m_environment.setBundledOutput( side, output );
                return null;
            }
            case 5:
            {
                // getBundledOutput
                int side = parseSide( args );
                return new Object[] { m_environment.getBundledOutput( side ) };
            }
            case 6:
            {
                // getBundledInput
                int side = parseSide( args );
                return new Object[] { m_environment.getBundledInput( side ) };
            }
            case 7:
            {
                // testBundledInput
                int side = parseSide( args );
                int mask = getInt( args, 1 );
                int input = m_environment.getBundledInput( side );
                return new Object[] { ((input & mask) == mask) };
            }
            case 8:
            case 9:
            {
                // setAnalogOutput/setAnalogueOutput
                int side = parseSide( args );
                int output = getInt( args, 1 );
                if( output < 0 || output > 15 )
                {
                    throw new LuaException( "Expected number in range 0-15" );
                }
                m_environment.setOutput( side, output );
                return null;
            }
            case 10:
            case 11:
            {
                // getAnalogOutput/getAnalogueOutput
                int side = parseSide( args );
                return new Object[] { m_environment.getOutput( side ) };
            }
            case 12:
            case 13:
            {
                // getAnalogInput/getAnalogueInput
                int side = parseSide( args );
                return new Object[] { m_environment.getInput( side ) };
            }
            default:
            {
                return null;
            }
        }
    }

    private int parseSide( Object[] args ) throws LuaException
    {
        String side = getString( args, 0 );
        for( int n = 0; n < IAPIEnvironment.SIDE_NAMES.length; n++ )
        {
            if( side.equals( IAPIEnvironment.SIDE_NAMES[n] ) )
            {
                return n;
            }
        }
        throw new LuaException( "Invalid side." );
    }
}
