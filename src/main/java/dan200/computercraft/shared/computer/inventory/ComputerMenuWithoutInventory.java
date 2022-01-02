/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.util.InvisibleSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Predicate;

/**
 * A computer menu which does not have any visible inventory.
 *
 * This adds invisible versions of the player's hotbars slots, to ensure they're synced to the client when changed.
 */
public class ComputerMenuWithoutInventory extends ContainerComputerBase
{
    public ComputerMenuWithoutInventory( MenuType<? extends ContainerComputerBase> type, int id, Inventory player, Predicate<Player> canUse, IComputer computer, ComputerFamily family )
    {
        super( type, id, canUse, computer, family );
        addSlots( player );
    }

    public ComputerMenuWithoutInventory( MenuType<? extends ContainerComputerBase> type, int id, Inventory player, ComputerContainerData data )
    {
        super( type, id, player, data );
        addSlots( player );
    }

    private void addSlots( Inventory player )
    {
        for( int i = 0; i < 9; i++ ) addSlot( new InvisibleSlot( player, i ) );
    }
}
