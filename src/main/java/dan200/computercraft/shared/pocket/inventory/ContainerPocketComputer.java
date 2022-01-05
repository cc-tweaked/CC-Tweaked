/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.inventory;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ContainerPocketComputer extends ContainerComputerBase
{
    private ContainerPocketComputer( int id, ServerComputer computer, ItemPocketComputer item, Hand hand )
    {
        super( ComputerCraftRegistry.ModContainers.POCKET_COMPUTER, id, p -> {
            ItemStack stack = p.getStackInHand( hand );
            return stack.getItem() == item && ItemPocketComputer.getServerComputer( stack ) == computer;
        }, computer, item.getFamily() );
    }

    public ContainerPocketComputer( int id, PlayerInventory player, PacketByteBuf packetByteBuf )
    {
        super( ComputerCraftRegistry.ModContainers.POCKET_COMPUTER, id, player, packetByteBuf );
    }

    public static class Factory implements ExtendedScreenHandlerFactory
    {
        private final ServerComputer computer;
        private final Text name;
        private final ItemPocketComputer item;
        private final Hand hand;

        public Factory( ServerComputer computer, ItemStack stack, ItemPocketComputer item, Hand hand )
        {
            this.computer = computer;
            name = stack.getName();
            this.item = item;
            this.hand = hand;
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
            return new ContainerPocketComputer( id, computer, item, hand );
        }

        @Override
        public void writeScreenOpeningData( ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf )
        {
            packetByteBuf.writeInt( computer.getInstanceID() );
            packetByteBuf.writeEnumConstant( computer.getFamily() );
        }
    }
}
