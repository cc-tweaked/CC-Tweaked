// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.detail.DetailRegistry;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.fluids.FluidStack;
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

    void registerGenericCapability(BlockCapability<?, Direction> capability);

    DetailRegistry<FluidStack> getFluidStackDetailRegistry();
}
