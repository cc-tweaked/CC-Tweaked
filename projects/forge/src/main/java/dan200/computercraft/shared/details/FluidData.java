// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.details;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Map;

public class FluidData {
    public static void fillBasic(Map<? super String, Object> data, FluidStack stack) {
        data.put("name", DetailHelpers.getId(BuiltInRegistries.FLUID, stack.getFluid()));
        data.put("amount", stack.getAmount());
        // "capacity" is added manually elsewhere since FluidStack does not contain a capacity.
        // See: dan200.computercraft.shared.peripheral.generic.methods.FluidMethods#tanks
    }

    public static void fill(Map<? super String, Object> data, FluidStack stack) {
        // FluidStack doesn't have a getTags method, so we need to use the deprecated builtInRegistryHolder.
        @SuppressWarnings("deprecation")
        var holder = stack.getFluid().builtInRegistryHolder();
        data.put("tags", DetailHelpers.getTags(holder));
    }
}
