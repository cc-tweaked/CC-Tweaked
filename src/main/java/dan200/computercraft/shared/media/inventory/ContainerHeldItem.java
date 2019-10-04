/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

import javax.annotation.Nonnull;

public class ContainerHeldItem extends Container
{
    private final ItemStack m_stack;
    private final EnumHand m_hand;

    public ContainerHeldItem( EntityPlayer player, EnumHand hand )
    {
        m_hand = hand;
        m_stack = player.getHeldItem( hand ).copy();
    }

    @Nonnull
    public ItemStack getStack()
    {
        return m_stack;
    }

    @Override
    public boolean canInteractWith( @Nonnull EntityPlayer player )
    {
        if( !player.isEntityAlive() ) return false;

        ItemStack stack = player.getHeldItem( m_hand );
        return stack == m_stack || !stack.isEmpty() && !m_stack.isEmpty() && stack.getItem() == m_stack.getItem();
    }
}
