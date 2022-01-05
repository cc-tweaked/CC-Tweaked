/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;

/**
 * A packet, which performs an action on a {@link ClientComputer}.
 */
public abstract class ComputerClientMessage implements NetworkMessage
{
    private final int instanceId;

    public ComputerClientMessage( int instanceId )
    {
        this.instanceId = instanceId;
    }

    public ComputerClientMessage( @Nonnull FriendlyByteBuf buf )
    {
        instanceId = buf.readVarInt();
    }

    public int getInstanceId()
    {
        return instanceId;
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeVarInt( instanceId );
    }

    public ClientComputer getComputer()
    {
        ClientComputer computer = ComputerCraft.clientComputerRegistry.get( instanceId );
        if( computer == null )
        {
            ComputerCraft.clientComputerRegistry.add( instanceId, computer = new ClientComputer( instanceId ) );
        }
        return computer;
    }
}
