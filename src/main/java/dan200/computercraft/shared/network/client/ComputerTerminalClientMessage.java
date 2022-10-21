/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

public class ComputerTerminalClientMessage implements NetworkMessage
{
    private final int containerId;
    private final TerminalState terminal;

    public ComputerTerminalClientMessage( Container menu, TerminalState terminal )
    {
        containerId = menu.containerId;
        this.terminal = terminal;
    }

    public ComputerTerminalClientMessage( @Nonnull PacketBuffer buf )
    {
        containerId = buf.readVarInt();
        terminal = new TerminalState( buf );
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeVarInt( containerId );
        terminal.write( buf );
    }

    @Override
    @OnlyIn( Dist.CLIENT )
    public void handle( NetworkEvent.Context context )
    {
        PlayerEntity player = Minecraft.getInstance().player;
        if( player != null && player.containerMenu.containerId == containerId && player.containerMenu instanceof ComputerMenu )
        {
            ((ComputerMenu) player.containerMenu).updateTerminal( terminal );
        }
    }
}
