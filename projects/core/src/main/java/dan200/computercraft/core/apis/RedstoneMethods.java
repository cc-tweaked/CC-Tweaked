// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.redstone.RedstoneAccess;

/**
 * A base class for all blocks with redstone integration.
 */
public class RedstoneMethods {
    private final RedstoneAccess redstone;

    public RedstoneMethods(RedstoneAccess redstone) {
        this.redstone = redstone;
    }

    /**
     * Turn the redstone signal of a specific side on or off.
     *
     * @param side The side to set.
     * @param on   Whether the redstone signal should be on or off. When on, a signal strength of 15 is emitted.
     */
    @LuaFunction
    public final void setOutput(ComputerSide side, boolean on) {
        redstone.setOutput(side, on ? 15 : 0);
    }

    /**
     * Get the current redstone output of a specific side.
     *
     * @param side The side to get.
     * @return Whether the redstone output is on or off.
     * @see #setOutput
     */
    @LuaFunction
    public final boolean getOutput(ComputerSide side) {
        return redstone.getOutput(side) > 0;
    }

    /**
     * Get the current redstone input of a specific side.
     *
     * @param side The side to get.
     * @return Whether the redstone input is on or off.
     */
    @LuaFunction
    public final boolean getInput(ComputerSide side) {
        return redstone.getInput(side) > 0;
    }

    /**
     * Set the redstone signal strength for a specific side.
     *
     * @param side  The side to set.
     * @param value The signal strength between 0 and 15.
     * @throws LuaException If {@code value} is not between 0 and 15.
     * @cc.since 1.51
     */
    @LuaFunction({ "setAnalogOutput", "setAnalogueOutput" })
    public final void setAnalogOutput(ComputerSide side, int value) throws LuaException {
        if (value < 0 || value > 15) throw new LuaException("Expected number in range 0-15");
        redstone.setOutput(side, value);
    }

    /**
     * Get the redstone output signal strength for a specific side.
     *
     * @param side The side to get.
     * @return The output signal strength, between 0 and 15.
     * @cc.since 1.51
     * @see #setAnalogOutput
     */
    @LuaFunction({ "getAnalogOutput", "getAnalogueOutput" })
    public final int getAnalogOutput(ComputerSide side) {
        return redstone.getOutput(side);
    }

    /**
     * Get the redstone input signal strength for a specific side.
     *
     * @param side The side to get.
     * @return The input signal strength, between 0 and 15.
     * @cc.since 1.51
     */
    @LuaFunction({ "getAnalogInput", "getAnalogueInput" })
    public final int getAnalogInput(ComputerSide side) {
        return redstone.getInput(side);
    }

    /**
     * Set the bundled cable output for a specific side.
     *
     * @param side   The side to set.
     * @param output The colour bitmask to set.
     * @cc.see colors.subtract For removing a colour from the bitmask.
     * @cc.see colors.combine For adding a color to the bitmask.
     */
    @LuaFunction
    public final void setBundledOutput(ComputerSide side, int output) {
        redstone.setBundledOutput(side, output);
    }

    /**
     * Get the bundled cable output for a specific side.
     *
     * @param side The side to get.
     * @return The bundle cable's output.
     */
    @LuaFunction
    public final int getBundledOutput(ComputerSide side) {
        return redstone.getBundledOutput(side);
    }

    /**
     * Get the bundled cable input for a specific side.
     *
     * @param side The side to get.
     * @return The bundle cable's input.
     * @see #testBundledInput To determine if a specific colour is set.
     */
    @LuaFunction
    public final int getBundledInput(ComputerSide side) {
        return redstone.getBundledInput(side);
    }

    /**
     * Determine if a specific combination of colours are on for the given side.
     *
     * @param side The side to test.
     * @param mask The mask to test.
     * @return If the colours are on.
     * @cc.usage Check if [`colors.white`] and [`colors.black`] are on above this block.
     * <pre>{@code
     * print(redstone.testBundledInput("top", colors.combine(colors.white, colors.black)))
     * }</pre>
     * @see #getBundledInput
     */
    @LuaFunction
    public final boolean testBundledInput(ComputerSide side, int mask) {
        var input = redstone.getBundledInput(side);
        return (input & mask) == mask;
    }
}
