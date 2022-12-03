/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl;

import dan200.computercraft.api.detail.BlockReference;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.media.MediaProvider;
import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.redstone.BundledRedstoneProvider;
import dan200.computercraft.api.turtle.TurtleRefuelHandler;
import dan200.computercraft.core.apis.ApiFactories;
import dan200.computercraft.core.asm.GenericMethod;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.impl.detail.DetailRegistryImpl;
import dan200.computercraft.impl.network.wired.WiredNodeImpl;
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
    public final int createUniqueNumberedSaveDir(MinecraftServer server, String parentSubPath) {
        return ServerContext.get(server).getNextId(parentSubPath);
    }

    @Override
    public final @Nullable WritableMount createSaveDirMount(MinecraftServer server, String subPath, long capacity) {
        var root = ServerContext.get(server).storageDir().toFile();
        try {
            return new FileMount(new File(root, subPath), capacity);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public final void registerGenericSource(GenericSource source) {
        GenericMethod.register(source);
    }

    @Override
    public final void registerBundledRedstoneProvider(BundledRedstoneProvider provider) {
        BundledRedstone.register(provider);
    }

    @Override
    public final int getBundledRedstoneOutput(Level world, BlockPos pos, Direction side) {
        return BundledRedstone.getDefaultOutput(world, pos, side);
    }

    @Override
    public final void registerMediaProvider(MediaProvider provider) {
        MediaProviders.register(provider);
    }

    @Override
    public final PacketNetwork getWirelessNetwork(MinecraftServer server) {
        return WirelessNetwork.getUniversal();
    }

    @Override
    public final void registerAPIFactory(ILuaAPIFactory factory) {
        ApiFactories.register(factory);
    }

    @Override
    public final WiredNode createWiredNodeForElement(WiredElement element) {
        return new WiredNodeImpl(element);
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
}
