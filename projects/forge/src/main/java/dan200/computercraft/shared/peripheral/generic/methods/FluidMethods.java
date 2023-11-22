// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.ForgeDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.shared.util.ArgumentHelpers.getRegistryEntry;

/**
 * Methods for interacting with tanks and other fluid storage blocks.
 *
 * @cc.module fluid_storage
 * @cc.see fluid_transportation For a detailed guide on how to use these methods.
 * @cc.since 1.94.0
 */
public class FluidMethods implements GenericPeripheral {
    @Override
    public PeripheralType getType() {
        return PeripheralType.ofAdditional("fluid_storage");
    }

    @Override
    public String id() {
        return ComputerCraftAPI.MOD_ID + ":fluid";
    }

    /**
     * Get all "tanks" in this fluid storage.
     * <p>
     * Each tank either contains some amount of fluid or is empty. Tanks with fluids inside will return some basic
     * information about the fluid, including its name and amount.
     * <p>
     * The returned table is sparse, and so empty tanks will be `nil` - it is recommended to loop over using `pairs`
     * rather than `ipairs`.
     *
     * @param fluids The current fluid handler.
     * @return All tanks.
     * @cc.treturn { (table|nil)... } All tanks in this fluid storage.
     */
    @LuaFunction(mainThread = true)
    public static Map<Integer, Map<String, ?>> tanks(IFluidHandler fluids) {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        var size = fluids.getTanks();
        for (var i = 0; i < size; i++) {
            var stack = fluids.getFluidInTank(i);
            if (!stack.isEmpty()) result.put(i + 1, ForgeDetailRegistries.FLUID_STACK.getBasicDetails(stack));
        }

        return result;
    }

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
    public static int pushFluid(
        IFluidHandler from, IComputerAccess computer,
        String toName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException {
        var fluid = fluidName.isPresent()
            ? getRegistryEntry(fluidName.get(), "fluid", RegistryWrappers.FLUIDS)
            : null;

        // Find location to transfer to
        var location = computer.getAvailablePeripheral(toName);
        if (location == null) throw new LuaException("Target '" + toName + "' does not exist");

        var to = extractHandler(location.getTarget());
        if (to == null) throw new LuaException("Target '" + toName + "' is not an tank");

        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        if (actualLimit <= 0) throw new LuaException("Limit must be > 0");

        return fluid == null
            ? moveFluid(from, actualLimit, to)
            : moveFluid(from, new FluidStack(fluid, actualLimit), to);
    }

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
    public static int pullFluid(
        IFluidHandler to, IComputerAccess computer,
        String fromName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException {
        var fluid = fluidName.isPresent()
            ? getRegistryEntry(fluidName.get(), "fluid", RegistryWrappers.FLUIDS)
            : null;

        // Find location to transfer to
        var location = computer.getAvailablePeripheral(fromName);
        if (location == null) throw new LuaException("Target '" + fromName + "' does not exist");

        var from = extractHandler(location.getTarget());
        if (from == null) throw new LuaException("Target '" + fromName + "' is not an tank");

        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        if (actualLimit <= 0) throw new LuaException("Limit must be > 0");

        return fluid == null
            ? moveFluid(from, actualLimit, to)
            : moveFluid(from, new FluidStack(fluid, actualLimit), to);
    }

    @Nullable
    private static IFluidHandler extractHandler(@Nullable Object object) {
        if (object instanceof BlockEntity blockEntity && blockEntity.isRemoved()) return null;

        if (object instanceof ICapabilityProvider provider) {
            var cap = provider.getCapability(ForgeCapabilities.FLUID_HANDLER);
            if (cap.isPresent()) return cap.orElseThrow(NullPointerException::new);
        }

        if (object instanceof IFluidHandler handler) return handler;
        return null;
    }

    /**
     * Move fluid from one handler to another.
     *
     * @param from  The handler to move from.
     * @param limit The maximum amount of fluid to move.
     * @param to    The handler to move to.
     * @return The amount of fluid moved.
     */
    private static int moveFluid(IFluidHandler from, int limit, IFluidHandler to) {
        return moveFluid(from, from.drain(limit, IFluidHandler.FluidAction.SIMULATE), limit, to);
    }

    /**
     * Move fluid from one handler to another.
     *
     * @param from  The handler to move from.
     * @param fluid The fluid and limit to move.
     * @param to    The handler to move to.
     * @return The amount of fluid moved.
     */
    private static int moveFluid(IFluidHandler from, FluidStack fluid, IFluidHandler to) {
        return moveFluid(from, from.drain(fluid, IFluidHandler.FluidAction.SIMULATE), fluid.getAmount(), to);
    }

    /**
     * Move fluid from one handler to another.
     *
     * @param from      The handler to move from.
     * @param extracted The fluid which is extracted from {@code from}.
     * @param limit     The maximum amount of fluid to move.
     * @param to        The handler to move to.
     * @return The amount of fluid moved.
     */
    private static int moveFluid(IFluidHandler from, FluidStack extracted, int limit, IFluidHandler to) {
        if (extracted == null || extracted.getAmount() <= 0) return 0;

        // Limit the amount to extract.
        extracted = extracted.copy();
        extracted.setAmount(Math.min(extracted.getAmount(), limit));

        var inserted = to.fill(extracted.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (inserted <= 0) return 0;

        // Remove the item from the original inventory. Technically this could fail, but there's little we can do
        // about that.
        extracted.setAmount(inserted);
        from.drain(extracted, IFluidHandler.FluidAction.EXECUTE);
        return inserted;
    }
}
