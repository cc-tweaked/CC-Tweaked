/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

public class ContainerViewComputer extends ComputerMenuWithoutInventory {
    public ContainerViewComputer(int id, Inventory player, ServerComputer computer) {
        super(Registry.ModContainers.VIEW_COMPUTER.get(), id, player, p -> canInteractWith(computer, p), computer, computer.getFamily());
    }

    public ContainerViewComputer(int id, Inventory player, ComputerContainerData data) {
        super(Registry.ModContainers.VIEW_COMPUTER.get(), id, player, data);
    }

    private static boolean canInteractWith(@Nonnull ServerComputer computer, @Nonnull Player player) {
        // If this computer no longer exists then discard it.
        if (ServerContext.get(computer.getLevel().getServer()).registry().get(computer.getInstanceID()) != computer) {
            return false;
        }

        // If we're a command computer then ensure we're in creative
        if (computer.getFamily() == ComputerFamily.COMMAND && !TileCommandComputer.isCommandUsable(player)) {
            return false;
        }

        return true;
    }
}
