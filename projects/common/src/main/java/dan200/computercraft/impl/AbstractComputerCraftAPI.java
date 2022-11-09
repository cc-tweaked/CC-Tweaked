/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl;

import dan200.computercraft.api.detail.BlockReference;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.detail.IDetailProvider;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.media.IMediaProvider;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import dan200.computercraft.api.turtle.TurtleRefuelHandler;
import dan200.computercraft.core.apis.ApiFactories;
import dan200.computercraft.core.asm.GenericMethod;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.impl.detail.DetailRegistryImpl;
import dan200.computercraft.impl.network.wired.WiredNode;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.details.BlockDetails;
import dan200.computercraft.shared.details.ItemDetails;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractComputerCraftAPI implements ComputerCraftAPIService {
    private final DetailRegistry<ItemStack> itemStackDetails = new DetailRegistryImpl<>(ItemDetails::fillBasic);
    private final DetailRegistry<BlockReference> blockDetails = new DetailRegistryImpl<>(BlockDetails::fillBasic);

    public static @Nullable InputStream getResourceFile(MinecraftServer server, String domain, String subPath) {
        var manager = server.getResourceManager();
        var resource = manager.getResource(new ResourceLocation(domain, subPath)).orElse(null);
        if (resource == null) return null;
        try {
            return resource.open();
        } catch (IOException ignored) {
            return null;
        }
    }

    @Override
    public final int createUniqueNumberedSaveDir(Level world, String parentSubPath) {
        var server = world.getServer();
        if (server == null) throw new IllegalArgumentException("Cannot find server from provided level");
        return ServerContext.get(server).getNextId(parentSubPath);
    }

    @Override
    public final @Nullable IWritableMount createSaveDirMount(Level world, String subPath, long capacity) {
        var server = world.getServer();
        if (server == null) throw new IllegalArgumentException("Cannot find server from provided level");

        try {
            return new FileMount(new File(ServerContext.get(server).storageDir().toFile(), subPath), capacity);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public final void registerGenericSource(GenericSource source) {
        GenericMethod.register(source);
    }

    @Override
    public final void registerBundledRedstoneProvider(IBundledRedstoneProvider provider) {
        BundledRedstone.register(provider);
    }

    @Override
    public final int getBundledRedstoneOutput(Level world, BlockPos pos, Direction side) {
        return BundledRedstone.getDefaultOutput(world, pos, side);
    }

    @Override
    public final void registerMediaProvider(IMediaProvider provider) {
        MediaProviders.register(provider);
    }

    @Override
    public final IPacketNetwork getWirelessNetwork() {
        return WirelessNetwork.getUniversal();
    }

    @Override
    public final void registerAPIFactory(ILuaAPIFactory factory) {
        ApiFactories.register(factory);
    }


    @Override
    public final IWiredNode createWiredNodeForElement(IWiredElement element) {
        return new WiredNode(element);
    }


    @Override
    public void registerRefuelHandler(TurtleRefuelHandler handler) {
        TurtleRefuelHandlers.register(handler);
    }

    @Override
    public final DetailRegistry<ItemStack> getItemStackDetailRegistry() {
        return itemStackDetails;
    }

    @Override
    public final DetailRegistry<BlockReference> getBlockInWorldDetailRegistry() {
        return blockDetails;
    }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T> void registerDetailProvider(Class<T> type, IDetailProvider<T> provider) {
        if (type == ItemStack.class) {
            itemStackDetails.addProvider((IDetailProvider<ItemStack>) provider);
        } else if (type == BlockReference.class) {
            blockDetails.addProvider((IDetailProvider<BlockReference>) provider);
        } else {
            throw new IllegalArgumentException("Unknown detail provider " + type);
        }
    }
}
