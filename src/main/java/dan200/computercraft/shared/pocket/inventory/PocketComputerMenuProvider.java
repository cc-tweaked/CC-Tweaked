/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.inventory;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
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

public class PocketComputerMenuProvider implements MenuProvider
{
    private final ServerComputer computer;
    private final Component name;
    private final ItemPocketComputer item;
    private final InteractionHand hand;
    private final boolean isTypingOnly;

    public PocketComputerMenuProvider( ServerComputer computer, ItemStack stack, ItemPocketComputer item, InteractionHand hand, boolean isTypingOnly )
    {
        this.computer = computer;
        name = stack.getHoverName();
        this.item = item;
        this.hand = hand;
        this.isTypingOnly = isTypingOnly;
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
        return new ComputerMenuWithoutInventory(
            isTypingOnly ? Registry.ModContainers.POCKET_COMPUTER_NO_TERM.get() : Registry.ModContainers.POCKET_COMPUTER.get(), id, inventory,
            p -> {
                ItemStack stack = p.getItemInHand( hand );
                return stack.getItem() == item && ItemPocketComputer.getServerComputer( stack ) == computer;
            },
            computer, item.getFamily()
        );
    }
}
