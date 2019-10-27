/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.client.*;
import dan200.computercraft.shared.network.server.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.function.Supplier;

public final class NetworkHandler
{
    public static SimpleNetworkWrapper network;

    private NetworkHandler()
    {
    }

    public static void setup()
    {
        network = NetworkRegistry.INSTANCE.newSimpleChannel( ComputerCraft.MOD_ID );

        // Server messages
        registerMainThread( 0, Side.SERVER, ComputerActionServerMessage::new );
        registerMainThread( 1, Side.SERVER, QueueEventServerMessage::new );
        registerMainThread( 2, Side.SERVER, RequestComputerMessage::new );
        registerMainThread( 3, Side.SERVER, KeyEventServerMessage::new );
        registerMainThread( 4, Side.SERVER, MouseEventServerMessage::new );

        // Client messages
        registerMainThread( 10, Side.CLIENT, ChatTableClientMessage::new );
        registerMainThread( 11, Side.CLIENT, ComputerDataClientMessage::new );
        registerMainThread( 12, Side.CLIENT, ComputerDeletedClientMessage::new );
        registerMainThread( 13, Side.CLIENT, ComputerTerminalClientMessage::new );
        registerMainThread( 14, Side.CLIENT, PlayRecordClientMessage::new );
    }

    public static void sendToPlayer( EntityPlayer player, IMessage packet )
    {
        network.sendTo( packet, (EntityPlayerMP) player );
    }

    public static void sendToAllPlayers( IMessage packet )
    {
        network.sendToAll( packet );
    }

    public static void sendToServer( IMessage packet )
    {
        network.sendToServer( packet );
    }

    public static void sendToAllAround( IMessage packet, NetworkRegistry.TargetPoint point )
    {
        network.sendToAllAround( packet, point );
    }

    /**
     * /**
     * Register packet, and a thread-unsafe handler for it.
     *
     * @param <T>     The type of the packet to send.
     * @param id      The identifier for this packet type
     * @param side    The side to register this packet handler under
     * @param factory The factory for this type of packet.
     */
    private static <T extends NetworkMessage> void registerMainThread( int id, Side side, Supplier<T> factory )
    {
        network.registerMessage( MAIN_THREAD_HANDLER, factory.get().getClass(), id, side );
    }

    private static final IMessageHandler<NetworkMessage, IMessage> MAIN_THREAD_HANDLER = ( packet, context ) -> {
        IThreadListener listener = context.side == Side.CLIENT ? Minecraft.getMinecraft() : context.getServerHandler().player.server;
        if( listener.isCallingFromMinecraftThread() )
        {
            packet.handle( context );
        }
        else
        {
            listener.addScheduledTask( () -> packet.handle( context ) );
        }
        return null;
    };
}
