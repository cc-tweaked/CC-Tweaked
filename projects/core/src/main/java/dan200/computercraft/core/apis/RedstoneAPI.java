// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.redstone.RedstoneAccess;

import java.util.List;

/**
 * Get and set redstone signals adjacent to this computer.
 * <p>
 * The {@link RedstoneAPI} library exposes three "types" of redstone control:
 * - Binary input/output ({@link #setOutput}/{@link #getInput}): These simply check if a redstone wire has any input or
 * output. A signal strength of 1 and 15 are treated the same.
 * - Analogue input/output ({@link #setAnalogOutput}/{@link #getAnalogInput}): These work with the actual signal
 * strength of the redstone wired, from 0 to 15.
 * - Bundled cables ({@link #setBundledOutput}/{@link #getBundledInput}): These interact with "bundled" cables, such
 * as those from Project:Red. These allow you to send 16 separate on/off signals. Each channel corresponds to a
 * colour, with the first being [`colors.white`] and the last [`colors.black`].
 * <p>
 * Whenever a redstone input changes, a [`event!redstone`] event will be fired. This may be used instead of repeativly
 * polling.
 * <p>
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
 * <p>
 * [comparator]: https://minecraft.wiki/w/Redstone_Comparator#Subtract_signal_strength "Redstone Comparator on
 * the Minecraft wiki."
 * @cc.module redstone
 */
public class RedstoneAPI extends RedstoneMethods implements ILuaAPI {
    public RedstoneAPI(RedstoneAccess environment) {
        super(environment);
    }

    @Override
    public String[] getNames() {
        return new String[]{ "rs", "redstone" };
    }

    /**
     * Returns a table containing the six sides of the computer. Namely, "top", "bottom", "left", "right", "front" and
     * "back".
     *
     * @return A table of valid sides.
     * @cc.since 1.2
     */
    @LuaFunction
    public final List<String> getSides() {
        return ComputerSide.NAMES;
    }
}
