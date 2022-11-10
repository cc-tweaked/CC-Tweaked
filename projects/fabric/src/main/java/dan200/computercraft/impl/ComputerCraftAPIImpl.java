/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.impl.detail.DetailRegistryImpl;
import dan200.computercraft.shared.ComputerCraft;
import dan200.computercraft.shared.computer.core.ResourceMount;
import dan200.computercraft.shared.details.FluidDetails;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.loader.api.FabricLoader;

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

    @Nullable
    @Override
    public IMount createResourceMount(String domain, String subPath) {
        if (ComputerCraft.server == null) return null;
        var mount = ResourceMount.get(domain, subPath, ComputerCraft.server.getResourceManager());
        return mount.exists("") ? mount : null;
    }

    @Override
    public DetailRegistry<StorageView<FluidVariant>> getFluidDetailRegistry() {
        return fluidDetails;
    }
}
