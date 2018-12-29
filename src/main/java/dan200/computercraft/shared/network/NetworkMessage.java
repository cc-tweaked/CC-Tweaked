/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network;

import dan200.computercraft.ComputerCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * The base interface for any message which will be sent to the client or server.
 *
 * @see dan200.computercraft.shared.network.client
 * @see dan200.computercraft.shared.network.server
 */
public interface NetworkMessage extends IMessage
{
    /**
     * The unique identifier for this packet type
     *
     * @return This packet type's identifier
     */
    int getId();

    /**
     * Write this packet to a buffer.
     *
     * This may be called on any thread, so this should be a pure operation.
     *
     * @param buf The buffer to write data to.
     */
    void toBytes( @Nonnull PacketBuffer buf );

    /**
     * Read this packet from a buffer.
     *
     * This may be called on any thread, so this should be a pure operation.
     *
     * @param buf The buffer to read data from.
     */
    void fromBytes( @Nonnull PacketBuffer buf );

    @Override
    default void fromBytes( ByteBuf buf )
    {
        fromBytes( new PacketBuffer( buf ) );
    }

    @Override
    default void toBytes( ByteBuf buf )
    {
        toBytes( new PacketBuffer( buf ) );
    }

    /**
     * Register a packet, and a thread-safe handler for it.
     *
     * @param side    The side to register this packet handler under
     * @param factory The factory for this type of packet.
     * @param handler The handler for this type of packet. Note, this may be called on any thread,
     *                and so should be thread-safe.
     */
    @SuppressWarnings( "unchecked" )
    static <T extends NetworkMessage> void register(
        Side side,
        Supplier<T> factory,
        BiConsumer<MessageContext, T> handler
    )
    {
        T instance = factory.get();
        ComputerCraft.networkWrapper.registerMessage( ( packet, ctx ) -> {
            handler.accept( ctx, (T) packet );
            return null;
        }, instance.getClass(), instance.getId(), side );
    }

    /**
     * Register packet, and a thread-unsafe handler for it.
     *
     * @param side    The side to register this packet handler under
     * @param factory The factory for this type of packet.
     * @param handler The handler for this type of packet. This will be called on the "main"
     *                thread (either client or server).
     */
    @SuppressWarnings( "unchecked" )
    static <T extends NetworkMessage> void registerMainThread(
        Side side,
        Supplier<T> factory,
        BiConsumer<MessageContext, T> handler
    )
    {
        T instance = factory.get();
        ComputerCraft.networkWrapper.registerMessage( ( packet, ctx ) -> {
            IThreadListener listener = side == Side.CLIENT ? Minecraft.getMinecraft() : ctx.getServerHandler().player.server;
            if( listener.isCallingFromMinecraftThread() )
            {
                handler.accept( ctx, (T) packet );
            }
            else
            {
                listener.addScheduledTask( () -> handler.accept( ctx, (T) packet ) );
            }
            return null;
        }, instance.getClass(), instance.getId(), side );
    }
}
