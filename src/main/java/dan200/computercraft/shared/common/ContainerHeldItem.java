/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.network.container.HeldItemContainerData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerHeldItem extends Container
{
    public static final ContainerType<ContainerHeldItem> PRINTOUT_TYPE = ContainerData.toType( HeldItemContainerData::new, ContainerHeldItem::createPrintout );

    private final ItemStack stack;
    private final Hand hand;

    public ContainerHeldItem( ContainerType<? extends ContainerHeldItem> type, int id, PlayerEntity player, Hand hand )
    {
        super( type, id );

        this.hand = hand;
        stack = player.getHeldItem( hand ).copy();
    }

    private static ContainerHeldItem createPrintout( int id, PlayerInventory inventory, HeldItemContainerData data )
    {
        return new ContainerHeldItem( PRINTOUT_TYPE, id, inventory.player, data.getHand() );
    }

    @Nonnull
    public ItemStack getStack()
    {
        return stack;
    }

    @Override
    public boolean canInteractWith( @Nonnull PlayerEntity player )
    {
        if( !player.isAlive() ) return false;

        ItemStack stack = player.getHeldItem( hand );
        return stack == this.stack || !stack.isEmpty() && !this.stack.isEmpty() && stack.getItem() == this.stack.getItem();
    }

    public static class Factory implements INamedContainerProvider
    {
        private final ContainerType<ContainerHeldItem> type;
        private final ITextComponent name;
        private final Hand hand;

        public Factory( ContainerType<ContainerHeldItem> type, ItemStack stack, Hand hand )
        {
            this.type = type;
            this.name = stack.getDisplayName();
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
        public Container createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
        {
            return new ContainerHeldItem( type, id, player, hand );
        }
    }
}
