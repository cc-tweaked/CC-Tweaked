/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

/**
 * A packet, which performs an action on the currently open {@link ComputerMenu}.
 */
public abstract class ComputerServerMessage implements NetworkMessage
{
    private final int containerId;

    protected ComputerServerMessage( AbstractContainerMenu menu )
    {
        containerId = menu.containerId;
    }

    public ComputerServerMessage( @Nonnull FriendlyByteBuf buffer )
    {
        containerId = buffer.readVarInt();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeVarInt( containerId );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        Player player = context.getSender();
        if( player.containerMenu.containerId == containerId && player.containerMenu instanceof ComputerMenu )
        {
            handle( context, (ComputerMenu) player.containerMenu );
        }
    }

    protected abstract void handle( NetworkEvent.Context context, @Nonnull ComputerMenu container );
}
