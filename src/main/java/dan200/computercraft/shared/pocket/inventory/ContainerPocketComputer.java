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
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ContainerPocketComputer extends ContainerComputerBase
{
    private ContainerPocketComputer( int id, ServerComputer computer, ItemPocketComputer item, InteractionHand hand )
    {
        super( Registry.ModContainers.POCKET_COMPUTER.get(), id, p -> {
            ItemStack stack = p.getItemInHand( hand );
            return stack.getItem() == item && ItemPocketComputer.getServerComputer( stack ) == computer;
        }, computer, item.getFamily() );
    }

    public ContainerPocketComputer( int id, Inventory player, ComputerContainerData data )
    {
        super( Registry.ModContainers.POCKET_COMPUTER.get(), id, player, data );
    }

    public static class Factory implements MenuProvider
    {
        private final ServerComputer computer;
        private final Component name;
        private final ItemPocketComputer item;
        private final InteractionHand hand;

        public Factory( ServerComputer computer, ItemStack stack, ItemPocketComputer item, InteractionHand hand )
        {
            this.computer = computer;
            name = stack.getHoverName();
            this.item = item;
            this.hand = hand;
        }


        @Nonnull
        @Override
        public Component getDisplayName()
        {
            return name;
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu( int id, @Nonnull Inventory inventory, @Nonnull Player entity )
        {
            return new ContainerPocketComputer( id, computer, item, hand );
        }
    }
}
