/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

/**
 * The base interface for any message which will be sent to the client or server.
 *
 * @see dan200.computercraft.shared.network.client
 * @see dan200.computercraft.shared.network.server
 */
public interface NetworkMessage
{
    /**
     * Write this packet to a buffer.
     *
     * This may be called on any thread, so this should be a pure operation.
     *
     * @param buf The buffer to write data to.
     */
    void toBytes( @Nonnull FriendlyByteBuf buf );

    /**
     * Handle this {@link NetworkMessage}.
     *
     * @param context The context with which to handle this message
     */
    void handle( NetworkEvent.Context context );
}
