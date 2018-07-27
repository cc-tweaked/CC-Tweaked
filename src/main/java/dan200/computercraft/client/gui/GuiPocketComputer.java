/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.media.inventory.ContainerHeldItem;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.minecraft.item.ItemStack;

public class GuiPocketComputer extends GuiComputer
{
    public GuiPocketComputer( ContainerHeldItem container )
    {
        super(
            container,
            getFamily( container.getStack() ),
            createClientComputer( container.getStack() ),
            ComputerCraft.terminalWidth_pocketComputer,
            ComputerCraft.terminalHeight_pocketComputer
        );
    }

    private static ComputerFamily getFamily( ItemStack stack )
    {
        return stack.getItem() instanceof ItemPocketComputer 
            ? ((ItemPocketComputer) stack.getItem()).getFamily( stack ) 
            : ComputerFamily.Normal;
    }

    private static ClientComputer createClientComputer( ItemStack stack )
    {
        return stack.getItem() instanceof ItemPocketComputer
            ? ((ItemPocketComputer) stack.getItem()).createClientComputer( stack )
            : null;
    }
}
