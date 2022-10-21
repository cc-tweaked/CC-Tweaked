/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl;

import dan200.computercraft.api.ComputerCraftAPI;
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
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
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

    int createUniqueNumberedSaveDir( @Nonnull World world, @Nonnull String parentSubPath );

    @Nullable
    IWritableMount createSaveDirMount( @Nonnull World world, @Nonnull String subPath, long capacity );

    @Nullable
    IMount createResourceMount( @Nonnull String domain, @Nonnull String subPath );

    void registerPeripheralProvider( @Nonnull IPeripheralProvider provider );

    void registerGenericSource( @Nonnull GenericSource source );

    void registerGenericCapability( @Nonnull Capability<?> capability );

    void registerTurtleUpgrade( @Nonnull ITurtleUpgrade upgrade );

    void registerBundledRedstoneProvider( @Nonnull IBundledRedstoneProvider provider );

    int getBundledRedstoneOutput( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction side );

    void registerMediaProvider( @Nonnull IMediaProvider provider );

    void registerPocketUpgrade( @Nonnull IPocketUpgrade upgrade );

    @Nonnull
    IPacketNetwork getWirelessNetwork();

    void registerAPIFactory( @Nonnull ILuaAPIFactory factory );

    <T> void registerDetailProvider( @Nonnull Class<T> type, @Nonnull IDetailProvider<T> provider );

    @Nonnull
    IWiredNode createWiredNodeForElement( @Nonnull IWiredElement element );

    @Nonnull
    LazyOptional<IWiredElement> getWiredElementAt( @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull Direction side );

    class Instance
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
