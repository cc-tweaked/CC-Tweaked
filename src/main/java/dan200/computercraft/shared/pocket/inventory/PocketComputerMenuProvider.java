/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.inventory;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
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

public class PocketComputerMenuProvider implements INamedContainerProvider
{
    private final ServerComputer computer;
    private final ITextComponent name;
    private final ItemPocketComputer item;
    private final Hand hand;
    private final boolean isTypingOnly;

    public PocketComputerMenuProvider( ServerComputer computer, ItemStack stack, ItemPocketComputer item, Hand hand, boolean isTypingOnly )
    {
        this.computer = computer;
        name = stack.getHoverName();
        this.item = item;
        this.hand = hand;
        this.isTypingOnly = isTypingOnly;
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
