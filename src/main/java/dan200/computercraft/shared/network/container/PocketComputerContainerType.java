/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Opens a pocket computer GUI based on the held item
 *
 * @see dan200.computercraft.shared.pocket.items.ItemPocketComputer
 */
public class PocketComputerContainerType implements ContainerType<ContainerPocketComputer>
{
    public static final ResourceLocation ID = new ResourceLocation( ComputerCraft.MOD_ID, "pocket_computer_gui" );

    public EnumHand hand;

    public PocketComputerContainerType( EnumHand hand )
    {
        this.hand = hand;
    }

    public PocketComputerContainerType()
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
