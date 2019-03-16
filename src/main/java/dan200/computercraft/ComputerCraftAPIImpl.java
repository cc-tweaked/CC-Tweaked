/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
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
import dan200.computercraft.shared.*;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import dan200.computercraft.shared.wired.WiredNode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import java.io.File;

public final class ComputerCraftAPIImpl implements IComputerCraftAPI
{
    public static final ComputerCraftAPIImpl INSTANCE = new ComputerCraftAPIImpl();

    private ComputerCraftAPIImpl()
    {
    }

    @Override
    public String getInstalledVersion()
    {
        return "${version}";
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
        return ComputerCraft.createResourceMount( domain, subPath );
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
    public int getBundledRedstoneOutput( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side )
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

    @Override
    public IWiredNode createWiredNodeForElement( @Nonnull IWiredElement element )
    {
        return new WiredNode( element );
    }

    @Override
    public IWiredElement getWiredElementAt( @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile == null ) return null;

        LazyOptional<IWiredElement> element = tile.getCapability( CapabilityWiredElement.CAPABILITY, side );
        return CapabilityWiredElement.unwrap( element );
    }
}
