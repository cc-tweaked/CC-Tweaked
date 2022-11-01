/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

public class ComputerTerminalClientMessage implements NetworkMessage
{
    private final int containerId;
    private final TerminalState terminal;

    public ComputerTerminalClientMessage( AbstractContainerMenu menu, TerminalState terminal )
    {
        containerId = menu.containerId;
        this.terminal = terminal;
    }

    public ComputerTerminalClientMessage( @Nonnull FriendlyByteBuf buf )
    {
        containerId = buf.readVarInt();
        terminal = new TerminalState( buf );
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeVarInt( containerId );
        terminal.write( buf );
    }

    @Override
    @OnlyIn( Dist.CLIENT )
    public void handle( NetworkEvent.Context context )
    {
        Player player = Minecraft.getInstance().player;
        if( player != null && player.containerMenu.containerId == containerId && player.containerMenu instanceof ComputerMenu menu )
        {
            menu.updateTerminal( terminal );
        }
    }
}
