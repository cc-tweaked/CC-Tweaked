/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.*;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerViewComputer extends Container implements IContainerComputer
{
    private final IComputer computer;
    private final InputState input = new InputState( this );

    public ContainerViewComputer( int id, IComputer computer )
    {
        super( null, id );
        this.computer = computer;
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        return computer;
    }

    @Override
    public boolean canUse( @Nonnull PlayerEntity player )
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
                if( server == null || !server.areCommandBlocksEnabled() )
                {
                    player.addChatMessage( new TranslatableComponent( "advMode.notEnabled" ), false );
                    return false;
                }
                else if( !player.isCreativeLevelTwoOp() )
                {
                    player.addChatMessage( new TranslatableComponent( "advMode.notAllowed" ), false );
                    return false;
                }
            }
        }

        return true;
    }

    @Nonnull
    @Override
    public InputState getInput()
    {
        return input;
    }

    @Override
    public void close( PlayerEntity player )
    {
        super.close( player );
        input.close();
    }
}
