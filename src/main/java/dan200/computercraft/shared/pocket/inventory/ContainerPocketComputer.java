/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.inventory;

import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.network.container.PocketComputerContainerData;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerPocketComputer extends ContainerHeldItem implements IContainerComputer
{
    public static final ContainerType<ContainerPocketComputer> TYPE = ContainerData.create( PocketComputerContainerData::new );

    private final InputState input = new InputState( this );

    public ContainerPocketComputer( int id, @Nonnull PlayerEntity player, Hand hand )
    {
        super( TYPE, id, player, hand );
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        ItemStack stack = getStack();
        return !stack.isEmpty() && stack.getItem() instanceof ItemPocketComputer
            ? ItemPocketComputer.getServerComputer( stack ) : null;
    }

    @Nonnull
    @Override
    public InputState getInput()
    {
        return input;
    }

    @Override
    public void onContainerClosed( PlayerEntity player )
    {
        super.onContainerClosed( player );
        input.close();
    }
}
