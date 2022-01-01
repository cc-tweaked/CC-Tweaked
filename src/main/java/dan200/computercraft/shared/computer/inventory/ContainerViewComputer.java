/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.container.ViewComputerContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

public class ContainerViewComputer extends ComputerMenuWithoutInventory
{
    private final int width;
    private final int height;

    public ContainerViewComputer( int id, Inventory player, ServerComputer computer )
    {
        super( Registry.ModContainers.VIEW_COMPUTER.get(), id, player, p -> canInteractWith( computer, p ), computer, computer.getFamily() );
        width = height = 0;
    }

    public ContainerViewComputer( int id, Inventory player, ViewComputerContainerData data )
    {
        super( Registry.ModContainers.VIEW_COMPUTER.get(), id, player, data );
        width = data.getWidth();
        height = data.getHeight();
    }

    private static boolean canInteractWith( @Nonnull ServerComputer computer, @Nonnull Player player )
    {
        // If this computer no longer exists then discard it.
        if( ComputerCraft.serverComputerRegistry.get( computer.getInstanceID() ) != computer )
        {
            return false;
        }

        // If we're a command computer then ensure we're in creative
        if( computer.getFamily() == ComputerFamily.COMMAND && !TileCommandComputer.isUsable( player ) )
        {
            return false;
        }

        return true;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }
}
