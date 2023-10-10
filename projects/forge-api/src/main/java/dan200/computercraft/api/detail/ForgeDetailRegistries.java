// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.detail;

import dan200.computercraft.impl.ComputerCraftAPIForgeService;
import net.minecraftforge.fluids.FluidStack;

/**
 * {@link DetailRegistry}s for Forge-specific types.
 */
public class ForgeDetailRegistries {
    /**
     * Provides details for {@link FluidStack}.
     */
    public static final DetailRegistry<FluidStack> FLUID_STACK = ComputerCraftAPIForgeService.get().getFluidStackDetailRegistry();
}
