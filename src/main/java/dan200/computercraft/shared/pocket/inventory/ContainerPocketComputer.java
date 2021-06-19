/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.inventory;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ContainerPocketComputer extends ContainerComputerBase
{
    private ContainerPocketComputer( int id, ServerComputer computer, ItemPocketComputer item, Hand hand )
    {
        super( Registry.ModContainers.POCKET_COMPUTER.get(), id, p -> {
            ItemStack stack = p.getItemInHand( hand );
            return stack.getItem() == item && ItemPocketComputer.getServerComputer( stack ) == computer;
        }, computer, item.getFamily() );
    }

    public ContainerPocketComputer( int id, PlayerInventory player, ComputerContainerData data )
    {
        super( Registry.ModContainers.POCKET_COMPUTER.get(), id, player, data );
    }

    public static class Factory implements INamedContainerProvider
    {
        private final ServerComputer computer;
        private final ITextComponent name;
        private final ItemPocketComputer item;
        private final Hand hand;

        public Factory( ServerComputer computer, ItemStack stack, ItemPocketComputer item, Hand hand )
        {
            this.computer = computer;
            name = stack.getHoverName();
            this.item = item;
            this.hand = hand;
        }


        @Nonnull
        @Override
        public ITextComponent getDisplayName()
        {
            return name;
        }

        @Nullable
        @Override
        public Container createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity entity )
        {
            return new ContainerPocketComputer( id, computer, item, hand );
        }
    }
}
