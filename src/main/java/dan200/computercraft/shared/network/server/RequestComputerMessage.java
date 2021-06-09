/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.server;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.NetworkMessage;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.network.PacketByteBuf;

import javax.annotation.Nonnull;

public class RequestComputerMessage implements NetworkMessage
{
    private int instance;

    public RequestComputerMessage( int instance )
    {
        this.instance = instance;
    }

    public RequestComputerMessage()
    {
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        buf.writeVarInt( this.instance );
    }

    @Override
    public void fromBytes( @Nonnull PacketByteBuf buf )
    {
        this.instance = buf.readVarInt();
    }

    @Override
    public void handle( PacketContext context )
    {
        ServerComputer computer = ComputerCraft.serverComputerRegistry.get( this.instance );
        if( computer != null )
        {
            computer.sendComputerState( context.getPlayer() );
        }
    }
}
