/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerComputer extends Container implements IContainerComputer
{
    private TileComputer m_computer;

    public ContainerComputer( int id, TileComputer computer )
    {
        super( null, id );
        m_computer = computer;
    }

    @Override
    public boolean canUse( @Nonnull PlayerEntity player )
    {
        return m_computer.isUsableByPlayer( player );
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        return m_computer.getServerComputer();
    }
}
