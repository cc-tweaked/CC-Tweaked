/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import javax.annotation.Nonnull;

import dan200.computercraft.shared.util.InventoryUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;

public class ContainerHeldItem extends ScreenHandler {
    private final ItemStack m_stack;
    private final Hand m_hand;

    public ContainerHeldItem(int id, PlayerEntity player, Hand hand) {
        super(null, id);
        this.m_hand = hand;
        this.m_stack = InventoryUtil.copyItem(player.getStackInHand(hand));
    }

    @Nonnull
    public ItemStack getStack() {
        return this.m_stack;
    }

    @Override
    public boolean canUse(@Nonnull PlayerEntity player) {
        if (!player.isAlive()) {
            return false;
        }

        ItemStack stack = player.getStackInHand(this.m_hand);
        return stack == this.m_stack || !stack.isEmpty() && !this.m_stack.isEmpty() && stack.getItem() == this.m_stack.getItem();
    }
}
