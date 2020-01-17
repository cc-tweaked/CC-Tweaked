/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.charset;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pl.asie.charset.api.wires.IBundledEmitter;
import pl.asie.charset.api.wires.IBundledReceiver;

public final class IntegrationCharset
{
    private static final ResourceLocation CAPABILITY_KEY = new ResourceLocation( ComputerCraft.MOD_ID, "charset" );

    @CapabilityInject( IBundledEmitter.class )
    static Capability<IBundledEmitter> CAPABILITY_EMITTER = null;

    @CapabilityInject( IBundledReceiver.class )
    static Capability<IBundledReceiver> CAPABILITY_RECEIVER = null;

    private IntegrationCharset()
    {
    }

    public static void register()
    {
        if( CAPABILITY_EMITTER == null || CAPABILITY_RECEIVER == null ) return;

        MinecraftForge.EVENT_BUS.register( IntegrationCharset.class );
        ComputerCraftAPI.registerBundledRedstoneProvider( new BundledRedstoneProvider() );
    }

    @SubscribeEvent
    public static void attachGenericCapabilities( AttachCapabilitiesEvent<TileEntity> event )
    {
        TileEntity tile = event.getObject();
        if( tile instanceof TileGeneric )
        {
            event.addCapability( CAPABILITY_KEY, new BundledCapabilityProvider( (TileGeneric) tile ) );
        }
    }
}
