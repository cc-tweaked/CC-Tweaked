/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.ComputerCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class ComputerDeletedClientMessage extends ComputerClientMessage
{
    public ComputerDeletedClientMessage( int instanceId )
    {
        super( instanceId );
    }

    public ComputerDeletedClientMessage( FriendlyByteBuf buffer )
    {
        super( buffer );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        ComputerCraft.clientComputerRegistry.remove( getInstanceId() );
    }
}
