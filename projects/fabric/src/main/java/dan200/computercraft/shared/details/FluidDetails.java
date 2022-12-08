/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.details;

import dan200.computercraft.api.detail.DetailProvider;
import dan200.computercraft.api.detail.FabricDetailRegistries;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;

import java.util.Map;

/**
 * {@link DetailProvider} support for fluids.
 *
 * @see FabricDetailRegistries#FLUID_VARIANT
 */
public class FluidDetails {
    public static void fillBasic(Map<? super String, Object> data, StorageView<FluidVariant> fluid) {
        data.put("name", DetailHelpers.getId(RegistryWrappers.FLUIDS, fluid.getResource().getFluid()));
        data.put("amount", fluid.getAmount());
    }

    public static void fill(Map<? super String, Object> data, StorageView<FluidVariant> fluid) {
        @SuppressWarnings("deprecation")
        var holder = fluid.getResource().getFluid().builtInRegistryHolder();
        data.put("tags", DetailHelpers.getTags(holder));
    }
}
