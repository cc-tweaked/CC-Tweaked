/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.util.PacketByteBuf;

import javax.annotation.Nonnull;

/**
 * A packet, which performs an action on a {@link ClientComputer}.
 */
public abstract class ComputerClientMessage implements NetworkMessage
{
    private int instanceId;

    public ComputerClientMessage( int instanceId )
    {
        this.instanceId = instanceId;
    }

    public ComputerClientMessage()
    {
    }

    public int getInstanceId()
    {
        return instanceId;
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        buf.writeVarInt( instanceId );
    }

    @Override
    public void fromBytes( @Nonnull PacketByteBuf buf )
    {
        instanceId = buf.readVarInt();
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
