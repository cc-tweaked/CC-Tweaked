/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class GuiPocketComputer extends GuiComputer<ContainerPocketComputer>
{
    public GuiPocketComputer( ContainerPocketComputer container, PlayerInventory player )
    {
        super(
            container, player,
            getFamily( container.getStack() ),
            ItemPocketComputer.createClientComputer( container.getStack() ),
            ComputerCraft.terminalWidth_pocketComputer,
            ComputerCraft.terminalHeight_pocketComputer
        );
    }

    private static ComputerFamily getFamily( ItemStack stack )
    {
        Item item = stack.getItem();
        return item instanceof ItemPocketComputer ? ((ItemPocketComputer) item).getFamily() : ComputerFamily.Normal;
    }
}
