// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.impl.detail.DetailRegistryImpl;
import dan200.computercraft.shared.details.FluidData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_WIRED_ELEMENT;

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
    public void registerPeripheralProvider(IPeripheralProvider provider) {
        Peripherals.register(provider);
    }

    @Override
    public void registerGenericCapability(Capability<?> capability) {
        Peripherals.registerGenericCapability(capability);
    }

    @Override
    public LazyOptional<WiredElement> getWiredElementAt(BlockGetter world, BlockPos pos, Direction side) {
        var tile = world.getBlockEntity(pos);
        return tile == null ? LazyOptional.empty() : tile.getCapability(CAPABILITY_WIRED_ELEMENT, side);
    }

    @Override
    public DetailRegistry<FluidStack> getFluidStackDetailRegistry() {
        return fluidStackDetails;
    }
}
