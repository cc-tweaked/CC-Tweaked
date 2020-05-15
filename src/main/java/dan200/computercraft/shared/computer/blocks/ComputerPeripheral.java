/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;

public class ComputerPeripheral implements IPeripheral
{
    private final String type;
    private final ComputerProxy computer;

    public ComputerPeripheral( String type, ComputerProxy computer )
    {
        this.type = type;
        this.computer = computer;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return type;
    }

    @LuaFunction
    public final void turnOn()
    {
        computer.turnOn();
    }

    @LuaFunction
    public final void shutdown()
    {
        computer.shutdown();
    }

    @LuaFunction
    public final void reboot()
    {
        computer.reboot();
    }

    @LuaFunction
    public final int getID()
    {
        return computer.assignID();
    }

    @LuaFunction
    public final boolean isOn()
    {
        return computer.isOn();
    }

    @LuaFunction
    public final String getLabel()
    {
        return computer.getLabel();
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof ComputerPeripheral && computer == ((ComputerPeripheral) other).computer;
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return computer.getTile();
    }
}
