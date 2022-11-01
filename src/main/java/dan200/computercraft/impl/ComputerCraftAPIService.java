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
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Backing interface for {@link ComputerCraftAPI}
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface ComputerCraftAPIService
{
    static ComputerCraftAPIService get()
    {
        ComputerCraftAPIService instance = Instance.INSTANCE;
        return instance == null ? Services.raise( ComputerCraftAPIService.class, Instance.ERROR ) : instance;
    }

    @Nonnull
    String getInstalledVersion();

    int createUniqueNumberedSaveDir( @Nonnull Level world, @Nonnull String parentSubPath );

    @Nullable
    IWritableMount createSaveDirMount( @Nonnull Level world, @Nonnull String subPath, long capacity );

    @Nullable
    IMount createResourceMount( @Nonnull String domain, @Nonnull String subPath );

    void registerPeripheralProvider( @Nonnull IPeripheralProvider provider );

    void registerGenericSource( @Nonnull GenericSource source );

    void registerGenericCapability( @Nonnull Capability<?> capability );

    void registerBundledRedstoneProvider( @Nonnull IBundledRedstoneProvider provider );

    int getBundledRedstoneOutput( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side );

    void registerMediaProvider( @Nonnull IMediaProvider provider );

    @Nonnull
    IPacketNetwork getWirelessNetwork();

    void registerAPIFactory( @Nonnull ILuaAPIFactory factory );

    @Deprecated
    <T> void registerDetailProvider( @Nonnull Class<T> type, @Nonnull IDetailProvider<T> provider );

    @Nonnull
    IWiredNode createWiredNodeForElement( @Nonnull IWiredElement element );

    @Nonnull
    LazyOptional<IWiredElement> getWiredElementAt( @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction side );

    void registerRefuelHandler( @Nonnull TurtleRefuelHandler handler );

    DetailRegistry<ItemStack> getItemStackDetailRegistry();

    DetailRegistry<BlockReference> getBlockInWorldDetailRegistry();

    DetailRegistry<FluidStack> getFluidStackDetailRegistry();

    final class Instance
    {
        static final @Nullable ComputerCraftAPIService INSTANCE;
        static final @Nullable Throwable ERROR;

        static
        {
            Services.LoadedService<ComputerCraftAPIService> helper = Services.tryLoad( ComputerCraftAPIService.class );
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance()
        {
        }
    }
}
