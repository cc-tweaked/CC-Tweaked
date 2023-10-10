// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
