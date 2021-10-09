/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.inventory;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PocketComputerMenuProvider implements NamedScreenHandlerFactory, ExtendedScreenHandlerFactory
{
    private final ServerComputer computer;
    private final Text name;
    private final ItemPocketComputer item;
    private final Hand hand;
    private final boolean isTypingOnly;

    public PocketComputerMenuProvider( ServerComputer computer, ItemStack stack, ItemPocketComputer item, Hand hand, boolean isTypingOnly )
    {
        this.computer = computer;
        name = stack.getName();
        this.item = item;
        this.hand = hand;
        this.isTypingOnly = isTypingOnly;
    }


    @Nonnull
    @Override
    public Text getDisplayName()
    {
        return name;
    }

    @Nullable
    @Override
    public ScreenHandler createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity entity )
    {
        return new ComputerMenuWithoutInventory(
            isTypingOnly ? ComputerCraftRegistry.ModContainers.POCKET_COMPUTER_NO_TERM : ComputerCraftRegistry.ModContainers.POCKET_COMPUTER, id, inventory,
            p -> {
                ItemStack stack = p.getStackInHand( hand );
                return stack.getItem() == item && ItemPocketComputer.getServerComputer( stack ) == computer;
            },
            computer, item.getFamily()
        );
    }

    @Override
    public void writeScreenOpeningData( ServerPlayerEntity player, PacketByteBuf packetByteBuf )
    {
        packetByteBuf.writeInt( computer.getInstanceID() );
        packetByteBuf.writeEnumConstant( computer.getFamily() );
    }
}
