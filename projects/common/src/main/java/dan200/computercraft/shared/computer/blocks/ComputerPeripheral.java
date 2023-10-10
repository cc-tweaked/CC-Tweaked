// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.OSAPI;

import javax.annotation.Nullable;

/**
 * A computer or turtle wrapped as a peripheral.
 * <p>
 * This allows for basic interaction with adjacent computers. Computers wrapped as peripherals will have the type
 * {@code computer} while turtles will be {@code turtle}.
 *
 * @cc.module computer
 */
public class ComputerPeripheral implements IPeripheral {
    private final String type;
    private final AbstractComputerBlockEntity owner;

    public ComputerPeripheral(String type, AbstractComputerBlockEntity owner) {
        this.type = type;
        this.owner = owner;
    }

    @Override
    public String getType() {
        return type;
    }

    /**
     * Turn the other computer on.
     */
    @LuaFunction
    public final void turnOn() {
        var computer = owner.getServerComputer();
        if (computer == null) {
            owner.startOn = true;
        } else {
            computer.turnOn();
        }
    }

    /**
     * Shutdown the other computer.
     */
    @LuaFunction
    public final void shutdown() {
        var computer = owner.getServerComputer();
        if (computer == null) {
            owner.startOn = false;
        } else {
            computer.shutdown();
        }
    }

    /**
     * Reboot or turn on the other computer.
     */
    @LuaFunction
    public final void reboot() {
        var computer = owner.getServerComputer();
        if (computer == null) {
            owner.startOn = true;
        } else {
            computer.reboot();
        }
    }

    /**
     * Get the other computer's ID.
     *
     * @return The computer's ID.
     * @see OSAPI#getComputerID() To get your computer's ID.
     */
    @LuaFunction
    public final int getID() {
        var computer = owner.getServerComputer();
        return computer == null ? owner.getComputerID() : computer.getID();
    }

    /**
     * Determine if the other computer is on.
     *
     * @return If the computer is on.
     */
    @LuaFunction
    public final boolean isOn() {
        var computer = owner.getServerComputer();
        return computer != null && computer.isOn();
    }

    /**
     * Get the other computer's label.
     *
     * @return The computer's label.
     * @see OSAPI#getComputerLabel() To get your label.
     */
    @Nullable
    @LuaFunction
    public final String getLabel() {
        var computer = owner.getServerComputer();
        return computer == null ? owner.getLabel() : computer.getLabel();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof ComputerPeripheral computerPeripheral && owner == computerPeripheral.owner;
    }

    @Override
    public Object getTarget() {
        return owner;
    }
}
