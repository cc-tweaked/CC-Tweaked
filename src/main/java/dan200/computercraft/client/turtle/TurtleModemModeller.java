/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.turtle;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class TurtleModemModeller implements TurtleUpgradeModeller<TurtleModem>
{
    private final ModelResourceLocation leftOffModel;
    private final ModelResourceLocation rightOffModel;
    private final ModelResourceLocation leftOnModel;
    private final ModelResourceLocation rightOnModel;

    public TurtleModemModeller( boolean advanced )
    {
        if( advanced )
        {
            leftOffModel = new ModelResourceLocation( "computercraft:turtle_modem_advanced_off_left", "inventory" );
            rightOffModel = new ModelResourceLocation( "computercraft:turtle_modem_advanced_off_right", "inventory" );
            leftOnModel = new ModelResourceLocation( "computercraft:turtle_modem_advanced_on_left", "inventory" );
            rightOnModel = new ModelResourceLocation( "computercraft:turtle_modem_advanced_on_right", "inventory" );
        }
        else
        {
            leftOffModel = new ModelResourceLocation( "computercraft:turtle_modem_normal_off_left", "inventory" );
            rightOffModel = new ModelResourceLocation( "computercraft:turtle_modem_normal_off_right", "inventory" );
            leftOnModel = new ModelResourceLocation( "computercraft:turtle_modem_normal_on_left", "inventory" );
            rightOnModel = new ModelResourceLocation( "computercraft:turtle_modem_normal_on_right", "inventory" );
        }
    }

    @Nonnull
    @Override
    public TransformedModel getModel( @Nonnull TurtleModem upgrade, @Nullable ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        boolean active = false;
        if( turtle != null )
        {
            CompoundTag turtleNBT = turtle.getUpgradeNBTData( side );
            active = turtleNBT.contains( "active" ) && turtleNBT.getBoolean( "active" );
        }

        return side == TurtleSide.LEFT
            ? TransformedModel.of( active ? leftOnModel : leftOffModel )
            : TransformedModel.of( active ? rightOnModel : rightOffModel );
    }
}
