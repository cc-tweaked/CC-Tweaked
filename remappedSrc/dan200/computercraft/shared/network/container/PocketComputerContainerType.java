/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import javax.annotation.Nonnull;

/**
 * Opens a pocket computer GUI based on the held item
 *
 * @see dan200.computercraft.shared.pocket.items.ItemPocketComputer
 */
public class PocketComputerContainerType implements ContainerType<ContainerPocketComputer>
{
    public static final Identifier ID = new Identifier( ComputerCraft.MOD_ID, "pocket_computer_gui" );

    public Hand hand;

    public PocketComputerContainerType( Hand hand )
    {
        this.hand = hand;
    }

    public PocketComputerContainerType()
    {
    }

    @Nonnull
    @Override
    public Identifier getId()
    {
        return ID;
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        buf.writeEnumConstant( hand );
    }

    @Override
    public void fromBytes( @Nonnull PacketByteBuf buf )
    {
        hand = buf.readEnumConstant( Hand.class );
    }
}
