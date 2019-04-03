/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.integration.mcmp;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.peripheral.modem.wireless.TileAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.TileWirelessModem;
import mcmultipart.api.addon.IMCMPAddon;
import mcmultipart.api.addon.MCMPAddon;
import mcmultipart.api.container.IMultipartContainer;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartRegistry;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.ref.MCMPCapabilities;
import mcmultipart.api.slot.EnumFaceSlot;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@MCMPAddon
public class MCMPIntegration implements IMCMPAddon
{
    private static final ResourceLocation CAPABILITY_KEY = new ResourceLocation( ComputerCraft.MOD_ID, "mcmultipart" );

    static final Map<Block, IMultipart> multipartMap = new HashMap<>();

    private static void register( IMultipartRegistry registry, Block block, IMultipart multipart )
    {
        registry.registerPartWrapper( block, multipart );
        multipartMap.put( block, multipart );
    }

    @Override
    public void registerParts( IMultipartRegistry registry )
    {
        // Setup all parts
        register( registry, ComputerCraft.Blocks.peripheral, new PartNormalModem() );
        register( registry, ComputerCraft.Blocks.advancedModem, new PartAdvancedModem() );

        // Subscribe to capability events
        MinecraftForge.EVENT_BUS.register( MCMPIntegration.class );

        // Register a peripheral provider
        ComputerCraftAPI.registerPeripheralProvider( ( world, pos, side ) ->
        {
            TileEntity tile = world.getTileEntity( pos );
            if( tile == null || !tile.hasCapability( MCMPCapabilities.MULTIPART_CONTAINER, null ) ) return null;
            IMultipartContainer container = tile.getCapability( MCMPCapabilities.MULTIPART_CONTAINER, null );
            if( container == null ) return null;

            IMultipartTile multipart = container.getPartTile( EnumFaceSlot.fromFace( side ) ).orElse( null );
            if( multipart == null ) return null;
            if( multipart instanceof IPeripheral ) return (IPeripheral) multipart;
            if( multipart instanceof IPeripheralTile ) return ((IPeripheralTile) multipart).getPeripheral( side );

            TileEntity underlying = multipart.getTileEntity();
            if( underlying instanceof IPeripheral ) return (IPeripheral) underlying;
            if( underlying instanceof IPeripheralTile ) return ((IPeripheralTile) underlying).getPeripheral( side );

            return null;
        } );
    }

    @SubscribeEvent
    public static void attach( AttachCapabilitiesEvent<TileEntity> event )
    {
        TileEntity tile = event.getObject();
        if( tile instanceof TileAdvancedModem || tile instanceof TileWirelessModem )
        {
            event.addCapability( CAPABILITY_KEY, new BasicMultipart( tile ) );
        }
    }

    private static final class BasicMultipart implements ICapabilityProvider
    {
        private final TileEntity tile;
        private IMultipartTile wrapped;

        private BasicMultipart( TileEntity tile ) {this.tile = tile;}

        @Override
        public boolean hasCapability( @Nonnull Capability<?> capability, @Nullable Direction facing )
        {
            return capability == MCMPCapabilities.MULTIPART_TILE;
        }

        @Nullable
        @Override
        public <T> T getCapability( @Nonnull Capability<T> capability, @Nullable Direction facing )
        {
            if( capability == MCMPCapabilities.MULTIPART_TILE )
            {
                IMultipartTile wrapped = this.wrapped;
                if( wrapped == null ) wrapped = this.wrapped = IMultipartTile.wrap( tile );
                return MCMPCapabilities.MULTIPART_TILE.cast( wrapped );
            }

            return null;
        }
    }
}
