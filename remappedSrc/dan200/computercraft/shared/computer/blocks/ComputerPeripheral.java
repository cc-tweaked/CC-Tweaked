/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;

public class ComputerPeripheral implements IPeripheral
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

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments )
    {
        switch( method )
        {
            case 0: // turnOn
                m_computer.turnOn();
                return null;
            case 1: // shutdown
                m_computer.shutdown();
                return null;
            case 2: // reboot
                m_computer.reboot();
                return null;
            case 3: // getID
                return new Object[] { m_computer.assignID() };
            case 4: // isOn
                return new Object[] { m_computer.isOn() };
            case 5: // getLabel
                return new Object[] { m_computer.getLabel() };
            default:
                return null;
        }
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other != null && other.getClass() == getClass();
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return m_computer.getTile();
    }
}
