/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerComputer extends Container
    implements IContainerComputer
{
    private TileComputer m_computer;

    public ContainerComputer( TileComputer computer )
    {
        m_computer = computer;
    }

    @Override
    public boolean canInteractWith( @Nonnull EntityPlayer player )
    {
        return m_computer.isUseableByPlayer( player );
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        return m_computer.getServerComputer();
    }
}
