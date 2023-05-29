// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * A Fabric-specific version of {@link ComputerCraftAPIService}.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface ComputerCraftAPIFabricService extends ComputerCraftAPIService {
    static ComputerCraftAPIFabricService get() {
        return (ComputerCraftAPIFabricService) ComputerCraftAPIService.get();
    }

    DetailRegistry<StorageView<FluidVariant>> getFluidDetailRegistry();

    <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> registerTurtleSerializer(ResourceLocation key, TurtleUpgradeSerialiser<T> serializer);
    <T extends IPocketUpgrade> PocketUpgradeSerialiser<T> registerPocketSerializer(ResourceLocation key, PocketUpgradeSerialiser<T> serializer);
}
