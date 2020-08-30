/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft;

import dan200.computercraft.api.ComputerCraftAPI.IComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.media.IMediaProvider;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.core.apis.ApiFactories;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.ResourceMount;
import dan200.computercraft.shared.*;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.wired.WiredNode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.swing.text.html.Option;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_WIRED_ELEMENT;

public final class ComputerCraftAPIImpl implements IComputerCraftAPI
{
    public static final ComputerCraftAPIImpl INSTANCE = new ComputerCraftAPIImpl();

    private String version;

    private ComputerCraftAPIImpl()
    {
    }

    public static InputStream getResourceFile( String domain, String subPath )
    {
        if (FabricLoader.getInstance().getGameInstance() instanceof MinecraftServer) {
            ReloadableResourceManager manager = (ReloadableResourceManager) ((MinecraftServer) FabricLoader.getInstance().getGameInstance()).getDataPackManager();
            try {
                return manager.getResource(new Identifier(domain, subPath)).getInputStream();
            } catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public String getInstalledVersion()
    {
        if( version != null ) return version;
        return version = FabricLoader.getInstance().getModContainer( ComputerCraft.MOD_ID )
            .map( x -> x.getMetadata().getVersion().toString() )
            .orElse( "unknown" );
    }

    @Override
    public int createUniqueNumberedSaveDir( @Nonnull World world, @Nonnull String parentSubPath )
    {
        return IDAssigner.getNextId( parentSubPath );
    }

    @Override
    public IWritableMount createSaveDirMount( @Nonnull World world, @Nonnull String subPath, long capacity )
    {
        try
        {
            return new FileMount( new File( IDAssigner.getDir(), subPath ), capacity );
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public IMount createResourceMount( @Nonnull String domain, @Nonnull String subPath )
    {
        if (FabricLoader.getInstance().getGameInstance() instanceof MinecraftServer) {
            ReloadableResourceManager manager = (ReloadableResourceManager) ((MinecraftServer) FabricLoader.getInstance().getGameInstance()).getDataPackManager();
            ResourceMount mount = ResourceMount.get(domain, subPath, manager);
            return mount.exists("") ? mount : null;
        }
        return null;
    }

    @Override
    public void registerPeripheralProvider( @Nonnull IPeripheralProvider provider )
    {
        Peripherals.register( provider );
    }

    @Override
    public void registerTurtleUpgrade( @Nonnull ITurtleUpgrade upgrade )
    {
        TurtleUpgrades.register( upgrade );
    }

    @Override
    public void registerBundledRedstoneProvider( @Nonnull IBundledRedstoneProvider provider )
    {
        BundledRedstone.register( provider );
    }

    @Override
    public int getBundledRedstoneOutput( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        return BundledRedstone.getDefaultOutput( world, pos, side );
    }

    @Override
    public void registerMediaProvider( @Nonnull IMediaProvider provider )
    {
        MediaProviders.register( provider );
    }

    @Override
    public void registerPocketUpgrade( @Nonnull IPocketUpgrade upgrade )
    {
        PocketUpgrades.register( upgrade );
    }

    @Nonnull
    @Override
    public IPacketNetwork getWirelessNetwork()
    {
        return WirelessNetwork.getUniversal();
    }

    @Override
    public void registerAPIFactory( @Nonnull ILuaAPIFactory factory )
    {
        ApiFactories.register( factory );
    }

    @Nonnull
    @Override
    public IWiredNode createWiredNodeForElement( @Nonnull IWiredElement element )
    {
        return new WiredNode( element );
    }

    @Nonnull
    @Override
    public Optional<IWiredElement> getWiredElementAt( @Nonnull BlockView world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        // TODO Fix this thing
//        BlockEntity tile = world.getBlockEntity( pos );
//        return tile == null ? Optional.empty() : tile.getCapability( CAPABILITY_WIRED_ELEMENT, side );
        return Optional.empty();
    }
}
