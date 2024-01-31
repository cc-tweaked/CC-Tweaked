// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.detail.ForgeDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.shared.util.ArgumentHelpers.getRegistryEntry;

/**
 * Fluid methods for Forge's {@link IFluidHandler}.
 */
public final class FluidMethods extends AbstractFluidMethods<IFluidHandler> {
    @Override
    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, ?>> tanks(IFluidHandler fluids) {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        var size = fluids.getTanks();
        for (var i = 0; i < size; i++) {
            var stack = fluids.getFluidInTank(i);
            if (!stack.isEmpty()) result.put(i + 1, ForgeDetailRegistries.FLUID_STACK.getBasicDetails(stack));
        }

        return result;
    }

    @Override
    @LuaFunction(mainThread = true)
    public int pushFluid(
        IFluidHandler from, IComputerAccess computer,
        String toName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException {
        var fluid = fluidName.isPresent()
            ? getRegistryEntry(fluidName.get(), "fluid", BuiltInRegistries.FLUID)
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

    @Override
    @LuaFunction(mainThread = true)
    public int pullFluid(
        IFluidHandler to, IComputerAccess computer,
        String fromName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException {
        var fluid = fluidName.isPresent()
            ? getRegistryEntry(fluidName.get(), "fluid", BuiltInRegistries.FLUID)
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
        if (object instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved()) return null;

            var level = blockEntity.getLevel();
            if (!(level instanceof ServerLevel serverLevel)) return null;

            var result = serverLevel.getCapability(Capabilities.FluidHandler.BLOCK, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null);
            if (result != null) return result;
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
        if (extracted.getAmount() <= 0) return 0;

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
