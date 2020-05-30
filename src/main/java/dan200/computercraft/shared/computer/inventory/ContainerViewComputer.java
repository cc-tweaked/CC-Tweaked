/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.core.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerViewComputer extends Container implements IContainerComputer
{
    private final IComputer computer;
    private final InputState input = new InputState( this );

    public ContainerViewComputer( IComputer computer )
    {
        this.computer = computer;
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        return computer;
    }

    @Override
    public boolean canInteractWith( @Nonnull EntityPlayer player )
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
            if( serverComputer.getFamily() == ComputerFamily.Command && !TileCommandComputer.isUsable( player ) )
            {
                return false;
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
    public void onContainerClosed( EntityPlayer player )
    {
        super.onContainerClosed( player );
        input.close();
    }
}
