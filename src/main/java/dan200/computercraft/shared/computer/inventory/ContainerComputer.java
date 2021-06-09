/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;

public class ContainerComputer extends ContainerComputerBase
{
    public ContainerComputer( int id, TileComputer tile )
    {
        super( ComputerCraftRegistry.ModContainers.COMPUTER, id, tile::isUsableByPlayer, tile.createServerComputer(), tile.getFamily() );
    }

    public ContainerComputer( int i, PlayerInventory playerInventory, PacketByteBuf packetByteBuf )
    {
        super( ComputerCraftRegistry.ModContainers.COMPUTER, i, playerInventory, packetByteBuf );
    }
}
