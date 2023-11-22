// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.PeripheralType;

import java.util.Map;
import java.util.Optional;

/**
 * Methods for interacting with tanks and other fluid storage blocks.
 *
 * @param <T> The type for fluid inventories.
 * @cc.module fluid_storage
 * @cc.since 1.94.0
 */
public abstract class AbstractFluidMethods<T> implements GenericPeripheral {
    @Override
    public final PeripheralType getType() {
        return PeripheralType.ofAdditional("fluid_storage");
    }

    @Override
    public final String id() {
        return ComputerCraftAPI.MOD_ID + ":fluid";
    }

    /**
     * Get all "tanks" in this fluid storage.
     * <p>
     * Each tank either contains some amount of fluid or is empty. Tanks with fluids inside will return some basic
     * information about the fluid, including its name and amount.
     * <p>
     * The returned table is sparse, and so empty tanks will be `nil` - it is recommended to loop over using [`pairs`]
     * rather than [`ipairs`].
     *
     * @param fluids The current fluid handler.
     * @return All tanks.
     * @cc.treturn { (table|nil)... } All tanks in this fluid storage.
     */
    @LuaFunction(mainThread = true)
    public abstract Map<Integer, Map<String, ?>> tanks(T fluids);

    /**
     * Move a fluid from one fluid container to another connected one.
     * <p>
     * This allows you to pull fluid in the current fluid container to another container <em>on the same wired
     * network</em>. Both containers must attached to wired modems which are connected via a cable.
     *
     * @param from      Container to move fluid from.
     * @param computer  The current computer.
     * @param toName    The name of the peripheral/container to push to. This is the string given to [`peripheral.wrap`],
     *                  and displayed by the wired modem.
     * @param limit     The maximum amount of fluid to move.
     * @param fluidName The fluid to move. If not given, an arbitrary fluid will be chosen.
     * @return The amount of moved fluid.
     * @throws LuaException If the peripheral to transfer to doesn't exist or isn't an fluid container.
     * @cc.see peripheral.getName Allows you to get the name of a [wrapped][`peripheral.wrap`] peripheral.
     */
    @LuaFunction(mainThread = true)
    public abstract int pushFluid(
        T from, IComputerAccess computer, String toName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException;

    /**
     * Move a fluid from a connected fluid container into this oneone.
     * <p>
     * This allows you to pull fluid in the current fluid container from another container <em>on the same wired
     * network</em>. Both containers must attached to wired modems which are connected via a cable.
     *
     * @param to        Container to move fluid to.
     * @param computer  The current computer.
     * @param fromName  The name of the peripheral/container to push to. This is the string given to [`peripheral.wrap`],
     *                  and displayed by the wired modem.
     * @param limit     The maximum amount of fluid to move.
     * @param fluidName The fluid to move. If not given, an arbitrary fluid will be chosen.
     * @return The amount of moved fluid.
     * @throws LuaException If the peripheral to transfer to doesn't exist or isn't an fluid container.
     * @cc.see peripheral.getName Allows you to get the name of a [wrapped][`peripheral.wrap`] peripheral.
     */
    @LuaFunction(mainThread = true)
    public abstract int pullFluid(
        T to, IComputerAccess computer, String fromName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException;
}
