/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

import javax.annotation.Nonnull;

/**
 * View an arbitrary computer on the client.
 *
 * @see dan200.computercraft.shared.command.CommandComputerCraft
 */
public class ViewComputerContainerType implements ContainerType<ContainerViewComputer>
{
    public static final Identifier ID = new Identifier( ComputerCraft.MOD_ID, "view_computer_gui" );

    public int instanceId;
    public int width;
    public int height;
    public ComputerFamily family;

    public ViewComputerContainerType( ServerComputer computer )
    {
        instanceId = computer.getInstanceID();
        Terminal terminal = computer.getTerminal();
        if( terminal != null )
        {
            width = terminal.getWidth();
            height = terminal.getHeight();
        }
        family = computer.getFamily();
    }

    public ViewComputerContainerType()
    {
    }

    @Nonnull
    @Override
    public Identifier getId()
    {
        return ID;
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        buf.writeVarInt( instanceId );
        buf.writeVarInt( width );
        buf.writeVarInt( height );
        buf.writeEnumConstant( family );
    }

    @Override
    public void fromBytes( @Nonnull PacketByteBuf buf )
    {
        instanceId = buf.readVarInt();
        width = buf.readVarInt();
        height = buf.readVarInt();
        family = buf.readEnumConstant( ComputerFamily.class );
    }
}
