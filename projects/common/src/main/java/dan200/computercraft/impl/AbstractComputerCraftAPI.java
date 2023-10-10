// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.BlockReference;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.media.MediaProvider;
import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.redstone.BundledRedstoneProvider;
import dan200.computercraft.api.turtle.TurtleRefuelHandler;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.core.filesystem.WritableFileMount;
import dan200.computercraft.impl.detail.DetailRegistryImpl;
import dan200.computercraft.impl.network.wired.WiredNodeImpl;
import dan200.computercraft.shared.computer.core.ResourceMount;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.details.BlockDetails;
import dan200.computercraft.shared.details.ItemDetails;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
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

    protected static final ResourceKey<Registry<TurtleUpgradeSerialiser<?>>> turtleUpgradeRegistryId = ResourceKey.createRegistryKey(new ResourceLocation(ComputerCraftAPI.MOD_ID, "turtle_upgrade_serialiser"));
    protected static final ResourceKey<Registry<PocketUpgradeSerialiser<?>>> pocketUpgradeRegistryId = ResourceKey.createRegistryKey(new ResourceLocation(ComputerCraftAPI.MOD_ID, "pocket_upgrade_serialiser"));

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
    public final WritableMount createSaveDirMount(MinecraftServer server, String subPath, long capacity) {
        var root = ServerContext.get(server).storageDir().toFile();
        return new WritableFileMount(new File(root, subPath), capacity);
    }

    @Override
    public final @Nullable Mount createResourceMount(MinecraftServer server, String domain, String subPath) {
        var mount = ResourceMount.get(domain, subPath, server.getResourceManager());
        return mount.exists("") ? mount : null;
    }

    @Override
    public final void registerGenericSource(GenericSource source) {
        GenericSources.register(source);
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
        return ServerContext.get(server).wirelessNetwork();
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
    public final void registerRefuelHandler(TurtleRefuelHandler handler) {
        TurtleRefuelHandlers.register(handler);
    }

    @Override
    public final ResourceKey<Registry<TurtleUpgradeSerialiser<?>>> turtleUpgradeRegistryId() {
        return turtleUpgradeRegistryId;
    }

    @Override
    public final ResourceKey<Registry<PocketUpgradeSerialiser<?>>> pocketUpgradeRegistryId() {
        return pocketUpgradeRegistryId;
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
