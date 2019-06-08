/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.common.ContainerHeldItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;

/**
 * Opens a printout GUI based on the currently held item
 *
 * @see dan200.computercraft.shared.media.items.ItemPrintout
 */
public class PrintoutContainerData implements ContainerData<ContainerHeldItem>
{
    private final Hand hand;

    public PrintoutContainerData( Hand hand )
    {
        this.hand = hand;
    }

    public PrintoutContainerData( PacketBuffer buffer )
    {
        hand = buffer.readEnumValue( Hand.class );
    }

    @Override
    public void toBytes( PacketBuffer buf )
    {
        buf.writeEnumValue( hand );
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName()
    {
        return new TranslationTextComponent( "gui.computercraft.printout" );
    }

    @Nonnull
    @Override
    public ContainerHeldItem createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        return new ContainerHeldItem( ContainerHeldItem.PRINTOUT_TYPE, id, player, hand );
    }
}
