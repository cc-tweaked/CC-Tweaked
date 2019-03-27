/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerComputer extends Container implements IContainerComputer
{
    private final TileComputer computer;
    private final InputState input = new InputState( this );

    public ContainerComputer( TileComputer computer )
    {
        this.computer = computer;
    }

    @Override
    public boolean canInteractWith( @Nonnull EntityPlayer player )
    {
        return computer.isUsableByPlayer( player );
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        return computer.getServerComputer();
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
