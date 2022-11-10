/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.detail;

import dan200.computercraft.impl.ComputerCraftAPIFabricService;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;

/**
 * {@link DetailRegistry}s for Fabric's types.
 *
 * @see VanillaDetailRegistries Detail registries for vanilla types.
 */
public class FabricDetailRegistries {
    /**
     * Detail provider for {@link FluidVariant}s.
     */
    public static final DetailRegistry<StorageView<FluidVariant>> FLUID_VARIANT = ComputerCraftAPIFabricService.get().getFluidDetailRegistry();
}
