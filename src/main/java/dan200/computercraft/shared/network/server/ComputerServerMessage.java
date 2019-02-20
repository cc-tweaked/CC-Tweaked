/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.server;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

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

    public ServerComputer getComputer( NetworkEvent.Context context )
    {
        ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instanceId );
        if( computer == null ) return null;

        // Verify the player is interacting with a computer.
        EntityPlayer player = context.getSender();
        if( player == null || !computer.isInteracting( player ) ) return null;

        return computer;
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        ServerComputer computer = getComputer( context );
        if( computer != null ) handle( computer );
    }

    protected abstract void handle( ServerComputer computer );
}
