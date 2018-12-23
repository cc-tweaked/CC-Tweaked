/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network;

import dan200.computercraft.ComputerCraft;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class PacketHandler
{
    @SubscribeEvent
    public void onClientPacket( FMLNetworkEvent.ClientCustomPacketEvent event )
    {
        try
        {
            ComputerCraftPacket packet = new ComputerCraftPacket();
            packet.fromBytes( event.getPacket().payload() );
            ComputerCraft.handlePacket( packet, null );
        }
        catch( Exception e )
        {
            ComputerCraft.log.error( "Error handling packet", e );
        }
    }

    @SubscribeEvent
    public void onServerPacket( FMLNetworkEvent.ServerCustomPacketEvent event )
    {
        try
        {
            ComputerCraftPacket packet = new ComputerCraftPacket();
            packet.fromBytes( event.getPacket().payload() );
            ComputerCraft.handlePacket( packet, ((NetHandlerPlayServer) event.getHandler()).player );
        }
        catch( Exception e )
        {
            ComputerCraft.log.error( "Error handling packet", e );
        }
    }
}
