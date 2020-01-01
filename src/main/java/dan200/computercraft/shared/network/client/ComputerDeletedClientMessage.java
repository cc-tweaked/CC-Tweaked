/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.ComputerCraft;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ComputerDeletedClientMessage extends ComputerClientMessage
{
    public ComputerDeletedClientMessage( int instanceId )
    {
        super( instanceId );
    }

    public ComputerDeletedClientMessage()
    {
    }

    @Override
    public void handle( MessageContext context )
    {
        ComputerCraft.clientComputerRegistry.remove( getInstanceId() );
    }
}
