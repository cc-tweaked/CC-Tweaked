/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.client.*;
import dan200.computercraft.shared.network.server.ComputerActionServerMessage;
import dan200.computercraft.shared.network.server.QueueEventServerMessage;
import dan200.computercraft.shared.network.server.RequestComputerMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.function.Function;
import java.util.function.Supplier;

public final class NetworkHandler
{
    public static SimpleChannel network;

    private NetworkHandler()
    {
    }

    public static void setup()
    {
        String version = ComputerCraft.getVersion();
        network = NetworkRegistry.ChannelBuilder.named( new ResourceLocation( ComputerCraft.MOD_ID, "network" ) )
            .networkProtocolVersion( () -> version )
            .clientAcceptedVersions( version::equals ).serverAcceptedVersions( version::equals )
            .simpleChannel();

        // Server messages
        registerMainThread( 0, ComputerActionServerMessage::new );
        registerMainThread( 1, QueueEventServerMessage::new );
        registerMainThread( 2, RequestComputerMessage::new );

        // Client messages
        registerMainThread( 10, ChatTableClientMessage::new );
        registerMainThread( 11, ComputerDataClientMessage::new );
        registerMainThread( 12, ComputerDeletedClientMessage::new );
        registerMainThread( 13, ComputerTerminalClientMessage::new );
        registerMainThread( 14, PlayRecordClientMessage.class, PlayRecordClientMessage::new );
    }

    public static void sendToPlayer( EntityPlayer player, NetworkMessage packet )
    {
        network.sendTo( packet, ((EntityPlayerMP) player).connection.netManager, NetworkDirection.PLAY_TO_CLIENT );
    }

    public static void sendToAllPlayers( NetworkMessage packet )
    {
        for( EntityPlayerMP player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers() )
        {
            sendToPlayer( player, packet );
        }
    }

    public static void sendToServer( NetworkMessage packet )
    {
        network.sendToServer( packet );
    }

    public static void sendToAllAround( NetworkMessage packet, World world, Vec3d pos, double range )
    {
        for( EntityPlayerMP player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers() )
        {
            if( player.getEntityWorld() != world ) continue;

            double x = pos.x - player.posX;
            double y = pos.y - player.posY;
            double z = pos.z - player.posZ;
            if( x * x + y * y + z * z < range * range ) sendToPlayer( player, packet );
        }
    }

    /**
     * /**
     * Register packet, and a thread-unsafe handler for it.
     *
     * @param id      The identifier for this packet type
     * @param factory The factory for this type of packet.
     */
    private static <T extends NetworkMessage> void registerMainThread( int id, Supplier<T> factory )
    {
        registerMainThread( id, getType( factory ), buf -> {
            T instance = factory.get();
            instance.fromBytes( buf );
            return instance;
        } );
    }

    /**
     * /**
     * Register packet, and a thread-unsafe handler for it.
     *
     * @param id      The identifier for this packet type
     * @param decoder The factory for this type of packet.
     */
    private static <T extends NetworkMessage> void registerMainThread( int id, Class<T> type, Function<PacketBuffer, T> decoder )
    {
        network.messageBuilder( type, id )
            .encoder( NetworkMessage::toBytes )
            .decoder( decoder )
            .consumer( ( packet, contextSup ) -> {
                NetworkEvent.Context context = contextSup.get();
                context.enqueueWork( () -> packet.handle( context ) );
                context.setPacketHandled( true );
            } )
            .add();
    }

    @SuppressWarnings( "unchecked" )
    private static <T> Class<T> getType( Supplier<T> supplier )
    {
        return (Class<T>) supplier.get().getClass();
    }
}
