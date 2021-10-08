/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.client.*;
import dan200.computercraft.shared.network.server.*;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.shedaniel.cloth.api.utils.v1.GameInstanceUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NetworkHandler
{
    private static final Int2ObjectMap<BiConsumer<PacketContext, PacketByteBuf>> packetReaders = new Int2ObjectOpenHashMap<>();
    private static final Object2IntMap<Class<?>> packetIds = new Object2IntOpenHashMap<>();

    private static final Identifier ID = new Identifier( ComputerCraft.MOD_ID, "main" );

    private NetworkHandler()
    {
    }

    public static void setup()
    {
        ServerSidePacketRegistry.INSTANCE.register( ID, NetworkHandler::receive );
        if( FabricLoader.getInstance()
            .getEnvironmentType() == EnvType.CLIENT )
        {
            ClientSidePacketRegistry.INSTANCE.register( ID, NetworkHandler::receive );
        }

        // Server messages
        registerMainThread( 0, ComputerActionServerMessage.class, ComputerActionServerMessage::new );
        registerMainThread( 1, QueueEventServerMessage.class, QueueEventServerMessage::new );
        registerMainThread( 2, RequestComputerMessage.class, RequestComputerMessage::new );
        registerMainThread( 3, KeyEventServerMessage.class, KeyEventServerMessage::new );
        registerMainThread( 4, MouseEventServerMessage.class, MouseEventServerMessage::new );
        registerMainThread( 5, UploadFileMessage.class, UploadFileMessage::new );
        registerMainThread( 6, ContinueUploadMessage.class, ContinueUploadMessage::new );

        // Client messages
        registerMainThread( 10, ChatTableClientMessage.class, ChatTableClientMessage::new );
        registerMainThread( 11, ComputerDataClientMessage.class, ComputerDataClientMessage::new );
        registerMainThread( 12, ComputerDeletedClientMessage.class, ComputerDeletedClientMessage::new );
        registerMainThread( 13, ComputerTerminalClientMessage.class, ComputerTerminalClientMessage::new );
        registerMainThread( 14, PlayRecordClientMessage.class, PlayRecordClientMessage::new );
        registerMainThread( 15, MonitorClientMessage.class, MonitorClientMessage::new );
        registerMainThread( 16, SpeakerPlayClientMessage.class, SpeakerPlayClientMessage::new );
        registerMainThread( 17, SpeakerStopClientMessage.class, SpeakerStopClientMessage::new );
        registerMainThread( 18, SpeakerMoveClientMessage.class, SpeakerMoveClientMessage::new );
        registerMainThread( 19, UploadResultMessage.class, UploadResultMessage::new );
    }

    private static void receive( PacketContext context, PacketByteBuf buffer )
    {
        int type = buffer.readByte();
        packetReaders.get( type )
            .accept( context, buffer );
    }

    /**
     * /** Register packet, and a thread-unsafe handler for it.
     *
     * @param <T>     The type of the packet to send.
     * @param type    The class of the type of packet to send.
     * @param id      The identifier for this packet type
     * @param decoder The factory for this type of packet.
     */
    private static <T extends NetworkMessage> void registerMainThread( int id, Class<T> type, Function<PacketByteBuf, T> decoder )
    {
        packetIds.put( type, id );
        packetReaders.put( id, ( context, buf ) -> {
            T result = decoder.apply( buf );
            context.getTaskQueue()
                .execute( () -> result.handle( context ) );
        } );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> Class<T> getType( Supplier<T> supplier )
    {
        return (Class<T>) supplier.get()
            .getClass();
    }

    private static PacketByteBuf encode( NetworkMessage message )
    {
        PacketByteBuf buf = new PacketByteBuf( Unpooled.buffer() );
        buf.writeByte( packetIds.getInt( message.getClass() ) );
        message.toBytes( buf );
        return buf;
    }

    public static void sendToPlayer( PlayerEntity player, NetworkMessage packet )
    {
        ((ServerPlayerEntity) player).networkHandler.sendPacket( new CustomPayloadS2CPacket( ID, encode( packet ) ) );
    }

    public static void sendToAllPlayers( NetworkMessage packet )
    {
        MinecraftServer server = GameInstanceUtils.getServer();
        server.getPlayerManager()
            .sendToAll( new CustomPayloadS2CPacket( ID, encode( packet ) ) );
    }

    public static void sendToAllPlayers( MinecraftServer server, NetworkMessage packet )
    {
        server.getPlayerManager()
            .sendToAll( new CustomPayloadS2CPacket( ID, encode( packet ) ) );
    }

    @Environment( EnvType.CLIENT )
    public static void sendToServer( NetworkMessage packet )
    {
        MinecraftClient.getInstance().player.networkHandler.sendPacket( new CustomPayloadC2SPacket( ID, encode( packet ) ) );
    }

    public static void sendToAllAround( NetworkMessage packet, World world, Vec3d pos, double range )
    {
        world.getServer()
            .getPlayerManager()
            .sendToAround( null, pos.x, pos.y, pos.z, range, world.getRegistryKey(), new CustomPayloadS2CPacket( ID, encode( packet ) ) );
    }

    public static void sendToAllTracking( NetworkMessage packet, WorldChunk chunk )
    {
        for( PlayerEntity player : chunk.getWorld().getPlayers() )
        {
            if ( chunk.getWorld().getRegistryKey().getValue() == player.getEntityWorld().getRegistryKey().getValue() && player.getChunkPos().equals( chunk.getPos() ) )
            {
                ((ServerPlayerEntity) player).networkHandler.sendPacket( new CustomPayloadS2CPacket( ID, encode( packet ) ) );
            }
        }
    }
}
