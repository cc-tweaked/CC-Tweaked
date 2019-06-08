/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.network.container.PrintoutContainerData;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import javax.annotation.Nonnull;

public class ContainerHeldItem extends Container
{
    public static final ContainerType<ContainerHeldItem> PRINTOUT_TYPE = ContainerData.create( PrintoutContainerData::new );

    private final ItemStack m_stack;
    private final Hand m_hand;

    public ContainerHeldItem( ContainerType<? extends ContainerHeldItem> type, int id, PlayerEntity player, Hand hand )
    {
        super( type, id );

        m_hand = hand;
        m_stack = InventoryUtil.copyItem( player.getHeldItem( hand ) );
    }

    @Nonnull
    public ItemStack getStack()
    {
        return m_stack;
    }

    @Override
    public boolean canInteractWith( @Nonnull PlayerEntity player )
    {
        if( !player.isAlive() ) return false;

        ItemStack stack = player.getHeldItem( m_hand );
        return stack == m_stack || !stack.isEmpty() && !m_stack.isEmpty() && stack.getItem() == m_stack.getItem();
    }
}
