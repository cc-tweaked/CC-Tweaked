/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.media.inventory.ContainerHeldItem;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;

public class GuiPocketComputer extends GuiComputer
{
    public GuiPocketComputer( ContainerHeldItem container )
    {
        super(
            container,
            ComputerCraft.Items.pocketComputer.getFamily( container.getStack() ),
            ItemPocketComputer.createClientComputer( container.getStack() ),
            ComputerCraft.terminalWidth_pocketComputer,
            ComputerCraft.terminalHeight_pocketComputer
        );
    }
}
