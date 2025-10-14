// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.detail.ForgeDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static dan200.computercraft.shared.util.ArgumentHelpers.getRegistryEntry;

/**
 * Fluid methods for Forge's fluid {@link ResourceHandler}.
 */
public final class FluidMethods extends AbstractFluidMethods<FluidMethods.StorageWrapper> {
    public record StorageWrapper(ResourceHandler<FluidResource> storage) {
    }

    @Override
    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, ?>> tanks(FluidMethods.StorageWrapper wrapper) {
        var storage = wrapper.storage();
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        var size = storage.size();
        for (var i = 0; i < size; i++) {
            var stack = storage.getResource(i).toStack(storage.getAmountAsInt(i));
            if (!stack.isEmpty()) result.put(i + 1, ForgeDetailRegistries.FLUID_STACK.getBasicDetails(stack));
        }

        return result;
    }

    @Override
    @LuaFunction(mainThread = true)
    public int pushFluid(
        FluidMethods.StorageWrapper from, IComputerAccess computer,
        String toName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException {
        var fluid = fluidName.isPresent()
            ? getRegistryEntry(fluidName.get(), "fluid", BuiltInRegistries.FLUID)
            : null;

        // Find location to transfer to
        var location = computer.getAvailablePeripheral(toName);
        if (location == null) throw new LuaException("Target '" + toName + "' does not exist");

        var to = extractHandler(location);
        if (to == null) throw new LuaException("Target '" + toName + "' is not an tank");

        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        if (actualLimit <= 0) throw new LuaException("Limit must be > 0");

        return moveFluid(from.storage(), to, fluid, actualLimit);
    }

    @Override
    @LuaFunction(mainThread = true)
    public int pullFluid(
        FluidMethods.StorageWrapper to, IComputerAccess computer,
        String fromName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException {
        var fluid = fluidName.isPresent()
            ? getRegistryEntry(fluidName.get(), "fluid", BuiltInRegistries.FLUID)
            : null;

        // Find location to transfer to
        var location = computer.getAvailablePeripheral(fromName);
        if (location == null) throw new LuaException("Target '" + fromName + "' does not exist");

        var from = extractHandler(location);
        if (from == null) throw new LuaException("Target '" + fromName + "' is not an tank");

        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        if (actualLimit <= 0) throw new LuaException("Limit must be > 0");

        return moveFluid(from, to.storage(), fluid, actualLimit);
    }

    @Nullable
    private static ResourceHandler<FluidResource> extractHandler(IPeripheral peripheral) {
        var object = peripheral.getTarget();
        var direction = peripheral instanceof dan200.computercraft.shared.peripheral.generic.GenericPeripheral sided ? sided.side() : null;

        if (object instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved()) return null;

            var level = blockEntity.getLevel();
            if (!(level instanceof ServerLevel serverLevel)) return null;

            var result = CapabilityUtil.getCapability(serverLevel, Capabilities.Fluid.BLOCK, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            if (result != null) return result;
        }

        return null;
    }

    /**
     * Move fluid from one handler to another.
     *
     * @param from  The handler to move from.
     * @param fluid The fluid to extract.
     * @param limit The maximum amount of fluid to move.
     * @param to    The handler to move to.
     * @return The amount of fluid moved.
     */
    private static int moveFluid(ResourceHandler<FluidResource> from, ResourceHandler<FluidResource> to, @Nullable Fluid fluid, int limit) {
        Predicate<FluidResource> predicate = fluid == null ? x -> true : x -> x.is(fluid);
        var moved = ResourceHandlerUtil.moveFirst(from, to, predicate, limit, null);
        return moved == null ? 0 : moved.amount();
    }
}
