// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.impl.detail.DetailRegistryImpl;
import dan200.computercraft.shared.ComputerCraft;
import dan200.computercraft.shared.details.FluidDetails;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

@AutoService(ComputerCraftAPIService.class)
public final class ComputerCraftAPIImpl extends AbstractComputerCraftAPI implements ComputerCraftAPIFabricService {
    private final DetailRegistry<StorageView<FluidVariant>> fluidDetails = new DetailRegistryImpl<>(FluidDetails::fillBasic);

    private @Nullable String version;

    @Override
    public String getInstalledVersion() {
        if (version != null) return version;
        return version = FabricLoader.getInstance().getModContainer(ComputerCraftAPI.MOD_ID)
            .map(x -> x.getMetadata().getVersion().toString())
            .orElse("unknown");
    }

    @Override
    public DetailRegistry<StorageView<FluidVariant>> getFluidDetailRegistry() {
        return fluidDetails;
    }

    @Override
    public <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> registerTurtleSerializer(ResourceLocation key, TurtleUpgradeSerialiser<T> serializer) {
        return Registry.register(ComputerCraft.TURTLE_UPGRADE_SERIALISERS, key, serializer);
    }

    @Override
    public <T extends IPocketUpgrade> PocketUpgradeSerialiser<T> registerPocketSerializer(ResourceLocation key, PocketUpgradeSerialiser<T> serializer) {
        return Registry.register(ComputerCraft.POCKET_UPGRADE_SERIALISERS, key, serializer);
    }
}
