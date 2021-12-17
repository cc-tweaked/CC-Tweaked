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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NetworkHandler
{
    private static final Int2ObjectMap<BiConsumer<PacketContext, FriendlyByteBuf>> packetReaders = new Int2ObjectOpenHashMap<>();
    private static final Object2IntMap<Class<?>> packetIds = new Object2IntOpenHashMap<>();

    private static final ResourceLocation ID = new ResourceLocation( ComputerCraft.MOD_ID, "main" );

    private NetworkHandler()
    {
    }

    public static void setup()
    {
        ServerPlayNetworking.registerGlobalReceiver( ID, NetworkHandler::receive );
        if( FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ) ClientHandler.setup();

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

    private static void receive( MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buffer, PacketSender sender )
    {
        int type = buffer.readByte();
        packetReaders.get( type ).accept( new PacketContext( player, server ), buffer );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> Class<T> getType( Supplier<T> supplier )
    {
        return (Class<T>) supplier.get().getClass();
    }

    private static FriendlyByteBuf encode( NetworkMessage message )
    {
        FriendlyByteBuf buf = new FriendlyByteBuf( Unpooled.buffer() );
        buf.writeByte( packetIds.getInt( message.getClass() ) );
        message.toBytes( buf );
        return buf;
    }

    public static void sendToPlayer( Player player, NetworkMessage packet )
    {
        ((ServerPlayer) player).connection.send( new ClientboundCustomPayloadPacket( ID, encode( packet ) ) );
    }

    public static void sendToAllPlayers( NetworkMessage packet )
    {
        MinecraftServer server = GameInstanceUtils.getServer();
        server.getPlayerList().broadcastAll( new ClientboundCustomPayloadPacket( ID, encode( packet ) ) );
    }

    public static void sendToServer( NetworkMessage packet )
    {
        Minecraft.getInstance().player.connection.send( new ServerboundCustomPayloadPacket( ID, encode( packet ) ) );
    }

    public static void sendToAllAround( NetworkMessage packet, Level world, Vec3 pos, double range )
    {
        world.getServer().getPlayerList().broadcast( null, pos.x, pos.y, pos.z, range, world.dimension(), new ClientboundCustomPayloadPacket( ID, encode( packet ) ) );
    }

    public static void sendToAllTracking( NetworkMessage packet, LevelChunk chunk )
    {
        Consumer<ServerPlayer> sender = p -> p.connection.send( new ClientboundCustomPayloadPacket( ID, encode( packet ) ) );
        ((ServerChunkCache) chunk.getLevel().getChunkSource()).chunkMap.getPlayers( chunk.getPos(), false ).forEach( sender );
    }

    /**
     * Register packet, and a thread-unsafe handler for it.
     *
     * @param <T>     The type of the packet to send.
     * @param type    The class of the type of packet to send.
     * @param id      The identifier for this packet type.
     * @param decoder The factory for this type of packet.
     */
    private static <T extends NetworkMessage> void registerMainThread( int id, Class<T> type, Function<FriendlyByteBuf, T> decoder )
    {
        packetIds.put( type, id );
        packetReaders.put( id, ( context, buf ) -> {
            T result = decoder.apply( buf );
            context.executor().execute( () -> result.handle( context ) );
        } );
    }

    private static class ClientHandler
    {
        static void setup()
        {
            ClientPlayNetworking.registerGlobalReceiver( ID, ClientHandler::receive );
        }

        static void receive( Minecraft client, ClientPacketListener handler, FriendlyByteBuf buffer, PacketSender responseSender )
        {
            int type = buffer.readByte();
            packetReaders.get( type ).accept( new PacketContext( client.player, client ), buffer );
        }
    }
}
