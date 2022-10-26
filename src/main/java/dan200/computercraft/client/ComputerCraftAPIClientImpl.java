/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;
import dan200.computercraft.impl.client.ComputerCraftAPIClientService;

import javax.annotation.Nonnull;

@AutoService( ComputerCraftAPIClientService.class )
public final class ComputerCraftAPIClientImpl implements ComputerCraftAPIClientService
{
    @Override
    public <T extends ITurtleUpgrade> void registerTurtleUpgradeModeller( @Nonnull TurtleUpgradeSerialiser<T> serialiser, @Nonnull TurtleUpgradeModeller<T> modeller )
    {
        TurtleUpgradeModellers.register( serialiser, modeller );
    }
}
