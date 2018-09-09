/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.api.lua.ICallContext;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ComputerPeripheral
    implements IPeripheral
{
    private final String m_type;
    private final ComputerProxy m_computer;

    public ComputerPeripheral( String type, ComputerProxy computer )
    {
        m_type = type;
        m_computer = computer;
    }

    // IPeripheral implementation

    @Nonnull
    @Override
    public String getType()
    {
        return m_type;
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "turnOn",
            "shutdown",
            "reboot",
            "getID",
            "isOn",
            "getLabel",
        };
    }

    @Nonnull
    @Override
    public MethodResult callMethod( @Nonnull IComputerAccess computer, @Nonnull ICallContext context, int method, @Nonnull Object[] arguments )
    {
        switch( method )
        {
            case 0:
            {
                // turnOn
                m_computer.turnOn();
                return MethodResult.empty();
            }
            case 1:
            {
                // shutdown
                m_computer.shutdown();
                return MethodResult.empty();
            }
            case 2:
            {
                // reboot
                m_computer.reboot();
                return MethodResult.empty();
            }
            case 3:
            {
                // getID
                return MethodResult.of( m_computer.assignID() );
            }
            case 4:
            {
                // isOn
                return MethodResult.of( m_computer.isOn() );
            }
            case 5:
                // getLabel
                return MethodResult.of( m_computer.getLabel() );
            default:
            {
                return MethodResult.empty();
            }
        }
    }

    @Nullable
    @Override
    @Deprecated
    public Object[] callMethod( @Nonnull IComputerAccess access, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        return callMethod( access, (ICallContext) context, method, arguments ).evaluate( context );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return (other != null && other.getClass() == this.getClass());
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return m_computer.getTile();
    }
}
