/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.inventory;

import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

import javax.annotation.Nullable;

public class ContainerPocketComputer extends ContainerHeldItem implements IContainerComputer
{
    public ContainerPocketComputer( EntityPlayer player, EnumHand hand )
    {
        super( player, hand );
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        ItemStack stack = getStack();
        return !stack.isEmpty() && stack.getItem() instanceof ItemPocketComputer
            ? ItemPocketComputer.getServerComputer( stack ) : null;
    }
}
