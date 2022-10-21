/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

/**
 * A packet, which performs an action on the currently open {@link ComputerMenu}.
 */
public abstract class ComputerServerMessage implements NetworkMessage
{
    private final int containerId;

    protected ComputerServerMessage( Container menu )
    {
        containerId = menu.containerId;
    }

    protected ComputerServerMessage( PacketBuffer buffer )
    {
        containerId = buffer.readVarInt();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeVarInt( containerId );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        PlayerEntity player = context.getSender();
        if( player.containerMenu.containerId == containerId && player.containerMenu instanceof ComputerMenu )
        {
            handle( context, (ComputerMenu) player.containerMenu );
        }
    }

    protected abstract void handle( NetworkEvent.Context context, @Nonnull ComputerMenu container );
}
