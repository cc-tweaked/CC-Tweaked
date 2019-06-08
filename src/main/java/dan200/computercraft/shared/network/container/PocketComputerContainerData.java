/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;

/**
 * Opens a pocket computer GUI based on the held item
 *
 * @see dan200.computercraft.shared.pocket.items.ItemPocketComputer
 */
public class PocketComputerContainerData implements ContainerData<ContainerPocketComputer>
{
    private final Hand hand;

    public PocketComputerContainerData( Hand hand )
    {
        this.hand = hand;
    }

    public PocketComputerContainerData( PacketBuffer buffer )
    {
        hand = buffer.readEnumValue( Hand.class );
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeEnumValue( hand );
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName()
    {
        return new TranslationTextComponent( "gui.computercraft.pocket" );
    }

    @Nonnull
    @Override
    public ContainerPocketComputer createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        return new ContainerPocketComputer( id, player, hand );
    }
}
