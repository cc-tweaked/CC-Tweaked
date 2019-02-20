/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.ContainerHeldItem;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Opens a printout GUI based on the currently held item
 *
 * @see dan200.computercraft.shared.media.items.ItemPrintout
 */
public class PrintoutContainerType implements ContainerType<ContainerHeldItem>
{
    public static final ResourceLocation ID = new ResourceLocation( ComputerCraft.MOD_ID, "printout_gui" );

    public EnumHand hand;

    public PrintoutContainerType( EnumHand hand )
    {
        this.hand = hand;
    }

    public PrintoutContainerType()
    {
    }

    @Nonnull
    @Override
    public ResourceLocation getId()
    {
        return ID;
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeEnumValue( hand );
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        hand = buf.readEnumValue( EnumHand.class );
    }
}
