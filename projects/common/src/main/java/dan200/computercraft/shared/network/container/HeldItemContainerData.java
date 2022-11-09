/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.common.HeldItemMenu;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

/**
 * Opens a printout GUI based on the currently held item.
 *
 * @see HeldItemMenu
 * @see PrintoutItem
 */
public class HeldItemContainerData implements ContainerData {
    private final InteractionHand hand;

    public HeldItemContainerData(InteractionHand hand) {
        this.hand = hand;
    }

    public HeldItemContainerData(FriendlyByteBuf buffer) {
        hand = buffer.readEnum(InteractionHand.class);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
    }

    public InteractionHand getHand() {
        return hand;
    }
}
