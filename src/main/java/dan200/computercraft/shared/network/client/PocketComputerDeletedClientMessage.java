/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

public class PocketComputerDeletedClientMessage implements NetworkMessage
{
    private final int instanceId;

    public PocketComputerDeletedClientMessage( int instanceId )
    {
        this.instanceId = instanceId;
    }

    public PocketComputerDeletedClientMessage( PacketBuffer buffer )
    {
        instanceId = buffer.readVarInt();
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeVarInt( instanceId );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        ClientPocketComputers.remove( instanceId );
    }
}
