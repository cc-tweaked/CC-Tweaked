/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.network.container.HeldItemContainerData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerHeldItem extends AbstractContainerMenu
{
    private final ItemStack stack;
    private final InteractionHand hand;

    public ContainerHeldItem( MenuType<? extends ContainerHeldItem> type, int id, Player player, InteractionHand hand )
    {
        super( type, id );

        this.hand = hand;
        stack = player.getItemInHand( hand )
            .copy();
    }

    public static ContainerHeldItem createPrintout( int id, Inventory inventory, HeldItemContainerData data )
    {
        return new ContainerHeldItem( ComputerCraftRegistry.ModContainers.PRINTOUT, id, inventory.player, data.getHand() );
    }

    @Nonnull
    public ItemStack getStack()
    {
        return stack;
    }

    @Override
    public boolean stillValid( @Nonnull Player player )
    {
        if( !player.isAlive() )
        {
            return false;
        }

        ItemStack stack = player.getItemInHand( hand );
        return stack == this.stack || !stack.isEmpty() && !this.stack.isEmpty() && stack.getItem() == this.stack.getItem();
    }

    public static class Factory implements ExtendedScreenHandlerFactory
    {
        private final MenuType<ContainerHeldItem> type;
        private final Component name;
        private final InteractionHand hand;

        public Factory( MenuType<ContainerHeldItem> type, ItemStack stack, InteractionHand hand )
        {
            this.type = type;
            name = stack.getHoverName();
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
        public AbstractContainerMenu createMenu( int id, @Nonnull Inventory inventory, @Nonnull Player player )
        {
            return new ContainerHeldItem( type, id, player, hand );
        }

        @Override
        public void writeScreenOpeningData( ServerPlayer serverPlayerEntity, FriendlyByteBuf packetByteBuf )
        {
            packetByteBuf.writeEnum( hand );
        }
    }
}
