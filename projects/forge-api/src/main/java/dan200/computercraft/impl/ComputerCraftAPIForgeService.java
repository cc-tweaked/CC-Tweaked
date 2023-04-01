// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
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

    void registerPeripheralProvider(IPeripheralProvider provider);

    void registerGenericCapability(Capability<?> capability);

    LazyOptional<WiredElement> getWiredElementAt(BlockGetter world, BlockPos pos, Direction side);

    DetailRegistry<FluidStack> getFluidStackDetailRegistry();
}
