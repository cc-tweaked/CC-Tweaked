/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.detail;

import dan200.computercraft.impl.ComputerCraftAPIService;
import net.minecraftforge.fluids.FluidStack;

/**
 * {@link DetailRegistry}s for Forge-specific types.
 */
public class ForgeDetailRegistries {
    /**
     * Provides details for {@link FluidStack}.
     */
    public static final DetailRegistry<FluidStack> FLUID_STACK = ComputerCraftAPIService.get().getFluidStackDetailRegistry();
}
