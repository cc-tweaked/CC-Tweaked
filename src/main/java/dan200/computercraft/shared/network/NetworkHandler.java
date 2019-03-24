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

    private static final int COMPUTER_ACTION_SERVER_MESSAGE = 0;
    private static final int QUEUE_EVENT_SERVER_MESSAGE = 1;
    private static final int REQUEST_COMPUTER_SERVER_MESSAGE = 2;
    private static final int KEY_EVENT_SERVER_MESSAGE = 3;
    private static final int MOUSE_EVENT_SERVER_MESSAGE = 4;

    private static final int CHAT_TABLE_CLIENT_MESSAGE = 10;
    private static final int COMPUTER_DATA_CLIENT_MESSAGE = 11;
    private static final int COMPUTER_DELETED_CLIENT_MESSAGE = 12;
    private static final int COMPUTER_TERMINAL_CLIENT_MESSAGE = 13;
    private static final int PLAY_RECORD_CLIENT_MESSAGE = 14;

    public static void setup()
    {
        network = NetworkRegistry.INSTANCE.newSimpleChannel( ComputerCraft.MOD_ID );

        // Server messages
        registerMainThread( NetworkHandler.COMPUTER_ACTION_SERVER_MESSAGE, Side.SERVER, ComputerActionServerMessage::new );
        registerMainThread( NetworkHandler.QUEUE_EVENT_SERVER_MESSAGE, Side.SERVER, QueueEventServerMessage::new );
        registerMainThread( NetworkHandler.REQUEST_COMPUTER_SERVER_MESSAGE, Side.SERVER, RequestComputerMessage::new );
        registerMainThread( NetworkHandler.KEY_EVENT_SERVER_MESSAGE, Side.SERVER, KeyEventServerMessage::new );
        registerMainThread( NetworkHandler.MOUSE_EVENT_SERVER_MESSAGE, Side.SERVER, MouseEventServerMessage::new );

        // Client messages
        registerMainThread( NetworkHandler.PLAY_RECORD_CLIENT_MESSAGE, Side.CLIENT, PlayRecordClientMessage::new );
        registerMainThread( NetworkHandler.COMPUTER_DATA_CLIENT_MESSAGE, Side.CLIENT, ComputerDataClientMessage::new );
        registerMainThread( NetworkHandler.COMPUTER_TERMINAL_CLIENT_MESSAGE, Side.CLIENT, ComputerTerminalClientMessage::new );
        registerMainThread( NetworkHandler.COMPUTER_DELETED_CLIENT_MESSAGE, Side.CLIENT, ComputerDeletedClientMessage::new );
        registerMainThread( NetworkHandler.CHAT_TABLE_CLIENT_MESSAGE, Side.CLIENT, ChatTableClientMessage::new );
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
