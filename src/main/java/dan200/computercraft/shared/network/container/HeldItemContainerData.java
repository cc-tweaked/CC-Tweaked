/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.common.ContainerHeldItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;

import javax.annotation.Nonnull;

/**
 * Opens a printout GUI based on the currently held item.
 *
 * @see ContainerHeldItem
 * @see dan200.computercraft.shared.media.items.ItemPrintout
 */
public class HeldItemContainerData implements ContainerData
{
    private final Hand hand;

    public HeldItemContainerData( Hand hand )
    {
        this.hand = hand;
    }

    public HeldItemContainerData( PacketByteBuf buffer )
    {
        hand = buffer.readEnumConstant( Hand.class );
    }

    @Override
    public void toBytes( PacketByteBuf buf )
    {
        buf.writeEnumConstant( hand );
    }

    @Nonnull
    public Hand getHand()
    {
        return hand;
    }
}
