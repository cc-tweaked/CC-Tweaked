/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.network.container.HeldItemContainerData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerHeldItem extends ScreenHandler
{
    private final ItemStack stack;
    private final Hand hand;

    public ContainerHeldItem( ScreenHandlerType<? extends ContainerHeldItem> type, int id, PlayerEntity player, Hand hand )
    {
        super( type, id );

        this.hand = hand;
        stack = player.getStackInHand( hand )
            .copy();
    }

    public static ContainerHeldItem createPrintout( int id, PlayerInventory inventory, PacketByteBuf data )
    {
        return createPrintout( id, inventory, new HeldItemContainerData( data ) );
    }

    public static ContainerHeldItem createPrintout( int id, PlayerInventory inventory, HeldItemContainerData data )
    {
        return new ContainerHeldItem( ComputerCraftRegistry.ModContainers.PRINTOUT, id, inventory.player, data.getHand() );
    }

    @Nonnull
    public ItemStack getStack()
    {
        return stack;
    }

    @Override
    public boolean canUse( @Nonnull PlayerEntity player )
    {
        if( !player.isAlive() )
        {
            return false;
        }

        ItemStack stack = player.getStackInHand( hand );
        return stack == this.stack || !stack.isEmpty() && !this.stack.isEmpty() && stack.getItem() == this.stack.getItem();
    }

    public static class Factory implements ExtendedScreenHandlerFactory
    {
        private final ScreenHandlerType<ContainerHeldItem> type;
        private final Text name;
        private final Hand hand;

        public Factory( ScreenHandlerType<ContainerHeldItem> type, ItemStack stack, Hand hand )
        {
            this.type = type;
            name = stack.getName();
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
        public ScreenHandler createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
        {
            return new ContainerHeldItem( type, id, player, hand );
        }

        @Override
        public void writeScreenOpeningData( ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf )
        {
            packetByteBuf.writeEnumConstant( hand );
        }
    }
}
