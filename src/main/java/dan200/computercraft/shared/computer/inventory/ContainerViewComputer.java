/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.*;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.network.container.ViewComputerContainerData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerViewComputer extends Container implements IContainerComputer
{
    public static final ContainerType<ContainerViewComputer> TYPE = ContainerData.create( ViewComputerContainerData::new );

    private final IComputer computer;
    private final InputState input = new InputState( this );

    public ContainerViewComputer( int id, IComputer computer )
    {
        super( TYPE, id );
        this.computer = computer;
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        return computer;
    }

    @Override
    public boolean canInteractWith( @Nonnull PlayerEntity player )
    {
        if( computer instanceof ServerComputer )
        {
            ServerComputer serverComputer = (ServerComputer) computer;

            // If this computer no longer exists then discard it.
            if( ComputerCraft.serverComputerRegistry.get( serverComputer.getInstanceID() ) != serverComputer )
            {
                return false;
            }

            // If we're a command computer then ensure we're in creative
            if( serverComputer.getFamily() == ComputerFamily.Command )
            {
                MinecraftServer server = player.getServer();
                if( server == null || !server.isCommandBlockEnabled() )
                {
                    player.sendStatusMessage( new TranslationTextComponent( "advMode.notEnabled" ), false );
                    return false;
                }
                else if( !player.canUseCommandBlock() )
                {
                    player.sendStatusMessage( new TranslationTextComponent( "advMode.notAllowed" ), false );
                    return false;
                }
            }
        }

        return computer != null;
    }

    @Nonnull
    @Override
    public InputState getInput()
    {
        return input;
    }

    @Override
    public void onContainerClosed( PlayerEntity player )
    {
        super.onContainerClosed( player );
        input.close();
    }
}
