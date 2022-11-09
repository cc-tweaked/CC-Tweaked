/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.blocks.CommandComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;


public class ViewComputerMenu extends ComputerMenuWithoutInventory {
    public ViewComputerMenu(int id, Inventory player, ServerComputer computer) {
        super(ModRegistry.Menus.VIEW_COMPUTER.get(), id, player, p -> canInteractWith(computer, p), computer, computer.getFamily());
    }

    public ViewComputerMenu(int id, Inventory player, ComputerContainerData data) {
        super(ModRegistry.Menus.VIEW_COMPUTER.get(), id, player, data);
    }

    private static boolean canInteractWith(ServerComputer computer, Player player) {
        // If this computer no longer exists then discard it.
        if (ServerContext.get(computer.getLevel().getServer()).registry().get(computer.getInstanceID()) != computer) {
            return false;
        }

        // If we're a command computer then ensure we're in creative
        if (computer.getFamily() == ComputerFamily.COMMAND && !CommandComputerBlockEntity.isCommandUsable(player)) {
            return false;
        }

        return true;
    }
}
