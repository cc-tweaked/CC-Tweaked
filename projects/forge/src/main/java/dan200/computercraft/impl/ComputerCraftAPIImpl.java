// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.impl.detail.DetailRegistryImpl;
import dan200.computercraft.shared.details.FluidData;
import dan200.computercraft.shared.peripheral.generic.ComponentLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Objects;

@AutoService(ComputerCraftAPIService.class)
public final class ComputerCraftAPIImpl extends AbstractComputerCraftAPI implements ComputerCraftAPIForgeService {
    private final DetailRegistry<FluidStack> fluidStackDetails = new DetailRegistryImpl<>(FluidData::fillBasic);

    private @Nullable String version;

    @Override
    public String getInstalledVersion() {
        if (version != null) return version;
        return version = ModList.get().getModContainerById(ComputerCraftAPI.MOD_ID)
            .map(x -> x.getModInfo().getVersion().toString())
            .orElse("unknown");
    }

    @Override
    public void registerGenericCapability(BlockCapability<?, Direction> capability) {
        Objects.requireNonNull(capability, "Capability cannot be null");
        Peripherals.addGenericLookup(new CapabilityLookup<>(capability));
    }

    @Override
    public DetailRegistry<FluidStack> getFluidStackDetailRegistry() {
        return fluidStackDetails;
    }

    /**
     * A {@link ComponentLookup} for {@linkplain BlockCapability capabilities}.
     * <p>
     * This is a record to ensure that adding the same capability multiple times only results in one lookup being
     * present in the resulting list.
     *
     * @param capability The capability to lookup
     * @param <T>        The type of the capability we look up.
     */
    private record CapabilityLookup<T>(BlockCapability<T, Direction> capability) implements ComponentLookup {
        @Nullable
        @Override
        public T find(ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, Direction side) {
            return level.getCapability(capability, pos, state, blockEntity, side);
        }
    }
}
