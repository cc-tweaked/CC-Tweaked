/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.OSAPI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A computer or turtle wrapped as a peripheral.
 *
 * This allows for basic interaction with adjacent computers. Computers wrapped as peripherals will have the type
 * {@code computer} while turtles will be {@code turtle}.
 *
 * @cc.module computer
 */
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

    /**
     * Turn the other computer on.
     */
    @LuaFunction
    public final void turnOn()
    {
        computer.turnOn();
    }

    /**
     * Shutdown the other computer.
     */
    @LuaFunction
    public final void shutdown()
    {
        computer.shutdown();
    }

    /**
     * Reboot or turn on the other computer.
     */
    @LuaFunction
    public final void reboot()
    {
        computer.reboot();
    }

    /**
     * Get the other computer's ID.
     *
     * @return The computer's ID.
     * @see OSAPI#getComputerID() To get your computer's ID.
     */
    @LuaFunction
    public final int getID()
    {
        return computer.assignID();
    }

    /**
     * Determine if the other computer is on.
     *
     * @return If the computer is on.
     */
    @LuaFunction
    public final boolean isOn()
    {
        return computer.isOn();
    }

    /**
     * Get the other computer's label.
     *
     * @return The computer's label.
     * @see OSAPI#getComputerLabel() To get your label.
     */
    @Nullable
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
