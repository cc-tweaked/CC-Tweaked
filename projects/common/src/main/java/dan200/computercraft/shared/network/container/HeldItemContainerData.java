// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
