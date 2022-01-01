/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

public class RequestComputerMessage implements NetworkMessage
{
    private final int instance;

    public RequestComputerMessage( int instance )
    {
        this.instance = instance;
    }

    public RequestComputerMessage( @Nonnull FriendlyByteBuf buf )
    {
        instance = buf.readVarInt();
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeVarInt( instance );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instance );
        if( computer != null ) computer.sendComputerState( context.getSender() );
    }
}
