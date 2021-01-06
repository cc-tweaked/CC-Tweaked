/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import net.minecraft.entity.player.PlayerInventory;

public class ContainerComputer extends ContainerComputerBase
{
    public ContainerComputer( int id, TileComputer tile )
    {
        super( Registry.ModContainers.COMPUTER.get(), id, tile::isUsableByPlayer, tile.createServerComputer(), tile.getFamily() );
    }

    public ContainerComputer( int id, PlayerInventory player, ComputerContainerData data )
    {
        super( Registry.ModContainers.COMPUTER.get(), id, player, data );
    }
}
