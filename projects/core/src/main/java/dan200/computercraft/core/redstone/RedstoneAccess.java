// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0
package dan200.computercraft.core.redstone;

import dan200.computercraft.core.apis.RedstoneMethods;
import dan200.computercraft.core.computer.ComputerSide;

/**
 * Common interface between blocks which provide and consume a redstone signal.
 *
 * @see RedstoneMethods Lua-facing methods wrapping this interface.
 * @see RedstoneState A concrete implementation of this class.
 */
public interface RedstoneAccess {
    /**
     * Set the redstone output on a given side.
     *
     * @param side   The side to set.
     * @param output The output level, between 0 and 15.
     */
    void setOutput(ComputerSide side, int output);

    /**
     * Get the redstone output on a given side.
     *
     * @param side The side to get.
     * @return The output level, between 0 and 15.
     */
    int getOutput(ComputerSide side);

    /**
     * Get the redstone input on a given side.
     *
     * @param side The side to get.
     * @return The input level, between 0 and 15.
     */
    int getInput(ComputerSide side);

    /**
     * Set the bundled redstone output on a given side.
     *
     * @param side   The side to set.
     * @param output The output state, as a 16-bit bitmask.
     */
    void setBundledOutput(ComputerSide side, int output);

    /**
     * Get the bundled redstone output on a given side.
     *
     * @param side The side to get.
     * @return The output state, as a 16-bit bitmask.
     */
    int getBundledOutput(ComputerSide side);

    /**
     * Set the bundled redstone input on a given side.
     *
     * @param side The side to get.
     * @return The input state, as a 16-bit bitmask.
     */
    int getBundledInput(ComputerSide side);
}
