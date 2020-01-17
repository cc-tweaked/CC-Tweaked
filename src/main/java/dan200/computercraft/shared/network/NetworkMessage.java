/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nonnull;

/**
 * The base interface for any message which will be sent to the client or server.
 *
 * @see dan200.computercraft.shared.network.client
 * @see dan200.computercraft.shared.network.server
 */
public interface NetworkMessage extends IMessage
{
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

    /**
     * Handle this {@link NetworkMessage}.
     *
     * @param context The context with which to handle this message
     */
    void handle( MessageContext context );

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
}
