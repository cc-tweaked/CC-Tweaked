/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

/**
 * A packet, which performs an action on a {@link ServerComputer}.
 *
 * This requires that the sending player is interacting with that computer via a
 * {@link IContainerComputer}.
 */
public abstract class ComputerServerMessage implements NetworkMessage
{
    private final int instanceId;

    public ComputerServerMessage( int instanceId )
    {
        this.instanceId = instanceId;
    }

    public ComputerServerMessage( @Nonnull FriendlyByteBuf buf )
    {
        instanceId = buf.readVarInt();
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeVarInt( instanceId );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instanceId );
        if( computer == null ) return;

        IContainerComputer container = computer.getContainer( context.getSender() );
        if( container == null ) return;

        handle( context, computer, container );
    }

    protected abstract void handle( NetworkEvent.Context context, @Nonnull ServerComputer computer, @Nonnull IContainerComputer container );
}
