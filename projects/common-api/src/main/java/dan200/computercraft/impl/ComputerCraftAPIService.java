/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.BlockReference;
import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.detail.IDetailProvider;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.media.IMediaProvider;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import dan200.computercraft.api.turtle.TurtleRefuelHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * Backing interface for {@link ComputerCraftAPI}
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface ComputerCraftAPIService {
    static ComputerCraftAPIService get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(ComputerCraftAPIService.class, Instance.ERROR) : instance;
    }

    String getInstalledVersion();

    int createUniqueNumberedSaveDir(Level world, String parentSubPath);

    @Nullable
    IWritableMount createSaveDirMount(Level world, String subPath, long capacity);

    @Nullable
    IMount createResourceMount(String domain, String subPath);

    @Deprecated
    void registerPeripheralProvider(IPeripheralProvider provider);

    void registerGenericSource(GenericSource source);

    @Deprecated
    void registerGenericCapability(Capability<?> capability);

    void registerBundledRedstoneProvider(IBundledRedstoneProvider provider);

    int getBundledRedstoneOutput(Level world, BlockPos pos, Direction side);

    void registerMediaProvider(IMediaProvider provider);

    IPacketNetwork getWirelessNetwork();

    void registerAPIFactory(ILuaAPIFactory factory);

    @Deprecated
    <T> void registerDetailProvider(Class<T> type, IDetailProvider<T> provider);

    IWiredNode createWiredNodeForElement(IWiredElement element);

    LazyOptional<IWiredElement> getWiredElementAt(BlockGetter world, BlockPos pos, Direction side);

    void registerRefuelHandler(TurtleRefuelHandler handler);

    DetailRegistry<ItemStack> getItemStackDetailRegistry();

    DetailRegistry<BlockReference> getBlockInWorldDetailRegistry();

    final class Instance {
        static final @Nullable ComputerCraftAPIService INSTANCE;
        static final @Nullable Throwable ERROR;

        static {
            var helper = Services.tryLoad(ComputerCraftAPIService.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance() {
        }
    }
}
