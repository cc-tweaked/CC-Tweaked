/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.server;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nonnull;

/**
 * A packet, which performs an action on a {@link ServerComputer}.
 *
 * This requires that the sending player is interacting with that computer via a
 * {@link dan200.computercraft.shared.computer.core.IContainerComputer}.
 */
public abstract class ComputerServerMessage implements NetworkMessage
{
    private int instanceId;

    public ComputerServerMessage( int instanceId )
    {
        this.instanceId = instanceId;
    }

    public ComputerServerMessage()
    {
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeVarInt( instanceId );
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        instanceId = buf.readVarInt();
    }

    @Override
    public void handle( MessageContext context )
    {
        ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instanceId );
        if( computer == null ) return;

        IContainerComputer container = computer.getContainer( context.getServerHandler().player );
        if( container == null ) return;

        handle( computer, container );
    }

    protected abstract void handle( @Nonnull ServerComputer computer, @Nonnull IContainerComputer container );
}
