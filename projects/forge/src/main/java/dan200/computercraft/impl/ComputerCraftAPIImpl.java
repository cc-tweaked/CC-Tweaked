/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.detail.IDetailProvider;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.impl.detail.DetailRegistryImpl;
import dan200.computercraft.shared.computer.core.ResourceMount;
import dan200.computercraft.shared.details.FluidData;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;

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
    public @Nullable IMount createResourceMount(String domain, String subPath) {
        var manager = ServerLifecycleHooks.getCurrentServer().getResourceManager();
        var mount = ResourceMount.get(domain, subPath, manager);
        return mount.exists("") ? mount : null;
    }

    @Override
    public void registerPeripheralProvider(IPeripheralProvider provider) {
        Peripherals.register(provider);
    }

    @Override
    public void registerGenericCapability(Capability<?> capability) {
        GenericPeripheralProvider.addCapability(capability);
    }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T> void registerDetailProvider(Class<T> type, IDetailProvider<T> provider) {
        if (type == FluidStack.class) {
            fluidStackDetails.addProvider((IDetailProvider<FluidStack>) provider);
        } else {
            super.registerDetailProvider(type, provider);
        }
    }

    @Override
    public LazyOptional<IWiredElement> getWiredElementAt(BlockGetter world, BlockPos pos, Direction side) {
        var tile = world.getBlockEntity(pos);
        return tile == null ? LazyOptional.empty() : tile.getCapability(CAPABILITY_WIRED_ELEMENT, side);
    }

    @Override
    public DetailRegistry<FluidStack> getFluidStackDetailRegistry() {
        return fluidStackDetails;
    }
}
