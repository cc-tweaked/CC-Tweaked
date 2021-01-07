/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.network;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a packet which may be sent across a {@link IPacketNetwork}.
 *
 * @see IPacketSender
 * @see IPacketNetwork#transmitSameDimension(Packet, double)
 * @see IPacketNetwork#transmitInterdimensional(Packet)
 * @see IPacketReceiver#receiveDifferentDimension(Packet)
 * @see IPacketReceiver#receiveSameDimension(Packet, double)
 */
public class Packet
{
    private final int channel;
    private final int replyChannel;
    private final Object payload;

    private final IPacketSender sender;

    /**
     * Create a new packet, ready for transmitting across the network.
     *
     * @param channel      The channel to send the packet along. Receiving devices should only process packets from on
     *                     channels they are listening to.
     * @param replyChannel The channel to reply on.
     * @param payload      The contents of this packet. This should be a "valid" Lua object, safe for queuing as an
     *                     event or returning from a peripheral call.
     * @param sender       The object which sent this packet.
     */
    public Packet( int channel, int replyChannel, @Nullable Object payload, @Nonnull IPacketSender sender )
    {
        Objects.requireNonNull( sender, "sender cannot be null" );

        this.channel = channel;
        this.replyChannel = replyChannel;
        this.payload = payload;
        this.sender = sender;
    }

    /**
     * Get the channel this packet is sent along. Receivers should generally only process packets from on channels they
     * are listening to.
     *
     * @return This packet's channel.
     */
    public int getChannel()
    {
        return channel;
    }

    /**
     * The channel to reply on. Objects which will reply should send it along this channel.
     *
     * @return This channel to reply on.
     */
    public int getReplyChannel()
    {
        return replyChannel;
    }

    /**
     * The actual data of this packet. This should be a "valid" Lua object, safe for queuing as an
     * event or returning from a peripheral call.
     *
     * @return The packet's payload
     */
    @Nullable
    public Object getPayload()
    {
        return payload;
    }

    /**
     * The object which sent this message.
     *
     * @return The sending object.
     */
    @Nonnull
    public IPacketSender getSender()
    {
        return sender;
    }

    @Override
    public boolean equals( Object o )
    {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        Packet packet = (Packet) o;

        if( channel != packet.channel ) return false;
        if( replyChannel != packet.replyChannel ) return false;
        if( !Objects.equals( payload, packet.payload ) ) return false;
        return sender.equals( packet.sender );
    }

    @Override
    public int hashCode()
    {
        int result;
        result = channel;
        result = 31 * result + replyChannel;
        result = 31 * result + (payload != null ? payload.hashCode() : 0);
        result = 31 * result + sender.hashCode();
        return result;
    }
}
