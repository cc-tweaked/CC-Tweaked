/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Queue an event on a {@link dan200.computercraft.shared.computer.core.ServerComputer}.
 *
 * @see dan200.computercraft.shared.computer.core.ClientComputer#queueEvent(String)
 * @see dan200.computercraft.shared.computer.core.ServerComputer#queueEvent(String)
 */
public class QueueEventServerMessage extends ComputerServerMessage
{
    private String event;
    private Object[] args;

    public QueueEventServerMessage( int instanceId, @Nonnull String event, @Nullable Object[] args )
    {
        super( instanceId );
        this.event = event;
        this.args = args;
    }

    public QueueEventServerMessage()
    {
    }

    @Override
    public int getId()
    {
        return NetworkMessages.QUEUE_EVENT_SERVER_MESSAGE;
    }

    @Nonnull
    public String getEvent()
    {
        return event;
    }

    @Nullable
    public Object[] getArgs()
    {
        return args;
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeString( event );
        buf.writeCompoundTag( args == null ? null : NBTUtil.encodeObjects( args ) );
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        super.fromBytes( buf );
        event = buf.readString( Short.MAX_VALUE );

        NBTTagCompound args = NBTUtil.readCompoundTag( buf );
        this.args = args == null ? null : NBTUtil.decodeObjects( args );
    }
}
