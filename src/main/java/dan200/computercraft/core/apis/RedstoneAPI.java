/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.computer.ComputerSide;

/**
 * Interact with redstone attached to this computer.
 *
 * The {@link RedstoneAPI} library exposes three "types" of redstone control:
 * - Binary input/output ({@link #setOutput}/{@link #getInput}): These simply check if a redstone wire has any input or
 * output. A signal strength of 1 and 15 are treated the same.
 * - Analogue input/output ({@link #setAnalogOutput}/{@link #getAnalogInput}): These work with the actual signal
 * strength of the redstone wired, from 0 to 15.
 * - Bundled cables ({@link #setBundledOutput}/{@link #getBundledInput}): These interact with "bundled" cables, such
 * as those from Project:Red. These allow you to send 16 separate on/off signals. Each channel corresponds to a
 * colour, with the first being @{colors.white} and the last @{colors.black}.
 *
 * Whenever a redstone input changes, a @{event!redstone} event will be fired. This may be used instead of repeativly
 * polling.
 *
 * This module may also be referred to as {@code rs}. For example, one may call {@code rs.getSides()} instead of
 * {@link #getSides}.
 *
 * @cc.usage Toggle the redstone signal above the computer every 0.5 seconds.
 *
 * <pre>{@code
 * while true do
 *   redstone.setOutput("top", not redstone.getOutput("top"))
 *   sleep(0.5)
 * end
 * }</pre>
 * @cc.usage Mimic a redstone comparator in [subtraction mode][comparator].
 *
 * <pre>{@code
 * while true do
 *   local rear = rs.getAnalogueInput("back")
 *   local sides = math.max(rs.getAnalogueInput("left"), rs.getAnalogueInput("right"))
 *   rs.setAnalogueOutput("front", math.max(rear - sides, 0))
 *
 *   os.pullEvent("redstone") -- Wait for a change to inputs.
 * end
 * }</pre>
 *
 * [comparator]: https://minecraft.gamepedia.com/Redstone_Comparator#Subtract_signal_strength "Redstone Comparator on
 * the Minecraft wiki."
 * @cc.module redstone
 */
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

    /**
     * Returns a table containing the six sides of the computer. Namely, "top", "bottom", "left", "right", "front" and
     * "back".
     *
     * @return A table of valid sides.
     * @cc.since 1.2
     */
    @LuaFunction
    public final String[] getSides()
    {
        return ComputerSide.NAMES;
    }

    /**
     * Turn the redstone signal of a specific side on or off.
     *
     * @param side The side to set.
     * @param on   Whether the redstone signal should be on or off. When on, a signal strength of 15 is emitted.
     */
    @LuaFunction
    public final void setOutput( ComputerSide side, boolean on )
    {
        environment.setOutput( side, on ? 15 : 0 );
    }

    /**
     * Get the current redstone output of a specific side.
     *
     * @param side The side to get.
     * @return Whether the redstone output is on or off.
     * @see #setOutput
     */
    @LuaFunction
    public final boolean getOutput( ComputerSide side )
    {
        return environment.getOutput( side ) > 0;
    }

    /**
     * Get the current redstone input of a specific side.
     *
     * @param side The side to get.
     * @return Whether the redstone input is on or off.
     */
    @LuaFunction
    public final boolean getInput( ComputerSide side )
    {
        return environment.getInput( side ) > 0;
    }

    /**
     * Set the redstone signal strength for a specific side.
     *
     * @param side  The side to set.
     * @param value The signal strength between 0 and 15.
     * @throws LuaException If {@code value} is not betwene 0 and 15.
     * @cc.since 1.51
     */
    @LuaFunction( { "setAnalogOutput", "setAnalogueOutput" } )
    public final void setAnalogOutput( ComputerSide side, int value ) throws LuaException
    {
        if( value < 0 || value > 15 ) throw new LuaException( "Expected number in range 0-15" );
        environment.setOutput( side, value );
    }

    /**
     * Get the redstone output signal strength for a specific side.
     *
     * @param side The side to get.
     * @return The output signal strength, between 0 and 15.
     * @cc.since 1.51
     * @see #setAnalogOutput
     */
    @LuaFunction( { "getAnalogOutput", "getAnalogueOutput" } )
    public final int getAnalogOutput( ComputerSide side )
    {
        return environment.getOutput( side );
    }

    /**
     * Get the redstone input signal strength for a specific side.
     *
     * @param side The side to get.
     * @return The input signal strength, between 0 and 15.
     * @cc.since 1.51
     */
    @LuaFunction( { "getAnalogInput", "getAnalogueInput" } )
    public final int getAnalogInput( ComputerSide side )
    {
        return environment.getInput( side );
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
    public final void setBundledOutput( ComputerSide side, int output )
    {
        environment.setBundledOutput( side, output );
    }

    /**
     * Get the bundled cable output for a specific side.
     *
     * @param side The side to get.
     * @return The bundle cable's output.
     */
    @LuaFunction
    public final int getBundledOutput( ComputerSide side )
    {
        return environment.getBundledOutput( side );
    }

    /**
     * Get the bundled cable input for a specific side.
     *
     * @param side The side to get.
     * @return The bundle cable's input.
     * @see #testBundledInput To determine if a specific colour is set.
     */
    @LuaFunction
    public final int getBundledInput( ComputerSide side )
    {
        return environment.getBundledInput( side );
    }

    /**
     * Determine if a specific combination of colours are on for the given side.
     *
     * @param side The side to test.
     * @param mask The mask to test.
     * @return If the colours are on.
     * @cc.usage Check if @{colors.white} and @{colors.black} are on above the computer.
     * <pre>{@code
     * print(redstone.testBundledInput("top", colors.combine(colors.white, colors.black)))
     * }</pre>
     * @see #getBundledInput
     */
    @LuaFunction
    public final boolean testBundledInput( ComputerSide side, int mask )
    {
        int input = environment.getBundledInput( side );
        return (input & mask) == mask;
    }
}
