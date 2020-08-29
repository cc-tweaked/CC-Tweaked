/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import javax.annotation.Nonnull;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.ContainerHeldItem;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

/**
 * Opens a printout GUI based on the currently held item
 *
 * @see dan200.computercraft.shared.media.items.ItemPrintout
 */
public class PrintoutContainerType implements ContainerType<ContainerHeldItem> {
    public static final Identifier ID = new Identifier(ComputerCraft.MOD_ID, "printout_gui");

    public Hand hand;

    public PrintoutContainerType(Hand hand) {
        this.hand = hand;
    }

    public PrintoutContainerType() {
    }

    @Override
    public void fromBytes(@Nonnull PacketByteBuf buf) {
        this.hand = buf.readEnumConstant(Hand.class);
    }

    @Nonnull
    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void toBytes(@Nonnull PacketByteBuf buf) {
        buf.writeEnumConstant(this.hand);
    }
}
