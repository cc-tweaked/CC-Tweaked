/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;

/**
 * View an arbitrary computer on the client.
 *
 * @see dan200.computercraft.shared.command.CommandComputerCraft
 */
public class ViewComputerContainerData implements ContainerData<ContainerViewComputer>
{
    public static final ResourceLocation ID = new ResourceLocation( ComputerCraft.MOD_ID, "view_computer_gui" );

    private final int instanceId;
    private final int width;
    private final int height;
    private final ComputerFamily family;

    public ViewComputerContainerData( ServerComputer computer )
    {
        instanceId = computer.getInstanceID();
        Terminal terminal = computer.getTerminal();
        if( terminal != null )
        {
            width = terminal.getWidth();
            height = terminal.getHeight();
        }
        else
        {
            width = height = 0;
        }
        family = computer.getFamily();
    }

    public ViewComputerContainerData( PacketBuffer buffer )
    {
        instanceId = buffer.readVarInt();
        width = buffer.readVarInt();
        height = buffer.readVarInt();
        family = buffer.readEnumValue( ComputerFamily.class );
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeVarInt( instanceId );
        buf.writeVarInt( width );
        buf.writeVarInt( height );
        buf.writeEnumValue( family );
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName()
    {
        return new TranslationTextComponent( "gui.computercraft.view_computer" );
    }

    @Nonnull
    @Override
    public ContainerViewComputer createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        IComputer computer = (player.world.isRemote ? ComputerCraft.clientComputerRegistry : ComputerCraft.serverComputerRegistry).get( id );
        return new ContainerViewComputer( id, computer );
    }
}
