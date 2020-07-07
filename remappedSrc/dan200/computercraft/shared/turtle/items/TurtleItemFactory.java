/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.ITurtleTile;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import javax.annotation.Nonnull;

public final class TurtleItemFactory
{
    private TurtleItemFactory() {}

    @Nonnull
    public static ItemStack create( ITurtleTile turtle )
    {
        ITurtleUpgrade leftUpgrade = turtle.getAccess().getUpgrade( TurtleSide.Left );
        ITurtleUpgrade rightUpgrade = turtle.getAccess().getUpgrade( TurtleSide.Right );

        String label = turtle.getLabel();
        if( label == null )
        {
            return create( -1, null, turtle.getColour(), turtle.getFamily(), leftUpgrade, rightUpgrade, 0, turtle.getOverlay() );
        }

        int id = turtle.getComputerID();
        int fuelLevel = turtle.getAccess().getFuelLevel();
        return create( id, label, turtle.getColour(), turtle.getFamily(), leftUpgrade, rightUpgrade, fuelLevel, turtle.getOverlay() );
    }

    @Nonnull
    public static ItemStack create( int id, String label, int colour, ComputerFamily family, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, int fuelLevel, Identifier overlay )
    {
        switch( family )
        {
            case Normal:
                return ComputerCraft.Items.turtleNormal.create( id, label, colour, leftUpgrade, rightUpgrade, fuelLevel, overlay );
            case Advanced:
                return ComputerCraft.Items.turtleAdvanced.create( id, label, colour, leftUpgrade, rightUpgrade, fuelLevel, overlay );
            default:
                return ItemStack.EMPTY;
        }
    }
}
