/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * A Forge-specific version of {@link ComputerCraftAPIService}.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 *
 * @see ForgeComputerCraftAPI
 */
@ApiStatus.Internal
public interface ComputerCraftAPIForgeService extends ComputerCraftAPIService {
    static ComputerCraftAPIForgeService get() {
        return (ComputerCraftAPIForgeService) ComputerCraftAPIService.get();
    }

    @Override
    void registerPeripheralProvider(IPeripheralProvider provider);

    @Override
    void registerGenericCapability(Capability<?> capability);

    DetailRegistry<FluidStack> getFluidStackDetailRegistry();
}
