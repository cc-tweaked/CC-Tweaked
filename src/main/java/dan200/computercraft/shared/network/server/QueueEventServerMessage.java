/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Queue an event on a {@link ServerComputer}.
 *
 * @see dan200.computercraft.shared.computer.core.ClientComputer#queueEvent(String)
 * @see ServerComputer#queueEvent(String)
 */
public class QueueEventServerMessage extends ComputerServerMessage
{
    private final String event;
    private final Object[] args;

    public QueueEventServerMessage( int instanceId, @Nonnull String event, @Nullable Object[] args )
    {
        super( instanceId );
        this.event = event;
        this.args = args;
    }

    public QueueEventServerMessage( @Nonnull PacketBuffer buf )
    {
        super( buf );
        event = buf.readUtf( Short.MAX_VALUE );

        CompoundNBT args = buf.readNbt();
        this.args = args == null ? null : NBTUtil.decodeObjects( args );
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeUtf( event );
        buf.writeNbt( args == null ? null : NBTUtil.encodeObjects( args ) );
    }

    @Override
    protected void handle( NetworkEvent.Context context, @Nonnull ServerComputer computer, @Nonnull IContainerComputer container )
    {
        computer.queueEvent( event, args );
    }
}
