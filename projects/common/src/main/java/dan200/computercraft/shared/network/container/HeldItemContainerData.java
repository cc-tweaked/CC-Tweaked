// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.common.HeldItemMenu;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;

/**
 * Opens a printout GUI based on the currently held item.
 *
 * @param hand The hand holding this item.
 * @see HeldItemMenu
 * @see PrintoutItem
 */
public record HeldItemContainerData(InteractionHand hand) implements ContainerData {
    public static final StreamCodec<RegistryFriendlyByteBuf, HeldItemContainerData> STREAM_CODEC = StreamCodec.composite(
        MoreStreamCodecs.ofEnum(InteractionHand.class), HeldItemContainerData::hand,
        HeldItemContainerData::new
    );

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        STREAM_CODEC.encode(buf, this);
    }
}
