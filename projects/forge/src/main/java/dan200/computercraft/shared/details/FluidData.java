// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.details;

import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraftforge.fluids.FluidStack;

import java.util.Map;

public class FluidData {
    public static void fillBasic(Map<? super String, Object> data, FluidStack stack) {
        data.put("name", DetailHelpers.getId(RegistryWrappers.FLUIDS, stack.getFluid()));
        data.put("amount", stack.getAmount());
    }

    public static void fill(Map<? super String, Object> data, FluidStack stack) {
        // FluidStack doesn't have a getTags method, so we need to use the deprecated builtInRegistryHolder.
        @SuppressWarnings("deprecation")
        var holder = stack.getFluid().builtInRegistryHolder();
        data.put("tags", DetailHelpers.getTags(holder));
    }
}
