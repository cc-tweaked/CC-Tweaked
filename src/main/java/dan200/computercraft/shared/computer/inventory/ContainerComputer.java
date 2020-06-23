/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.network.container.ContainerData;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

public class ContainerComputer extends ContainerComputerBase
{
    public static final ContainerType<ContainerComputer> TYPE = ContainerData.toType( ComputerContainerData::new, ContainerComputer::new );

    public ContainerComputer( int id, TileComputer tile )
    {
        super( TYPE, id, tile::isUsableByPlayer, tile.createServerComputer(), tile.getFamily() );
    }

    private ContainerComputer( int id, PlayerInventory player, ComputerContainerData data )
    {
        super( TYPE, id, player, data );
    }
}
