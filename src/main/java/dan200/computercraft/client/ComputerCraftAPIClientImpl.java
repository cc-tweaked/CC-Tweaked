/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import dan200.computercraft.api.client.ComputerCraftAPIClient;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;

import javax.annotation.Nonnull;

public final class ComputerCraftAPIClientImpl implements ComputerCraftAPIClient.IComputerCraftAPIClient
{
    public static final ComputerCraftAPIClientImpl INSTANCE = new ComputerCraftAPIClientImpl();

    private ComputerCraftAPIClientImpl()
    {
    }

    @Override
    public <T extends ITurtleUpgrade> void registerTurtleUpgradeModeller( @Nonnull TurtleUpgradeSerialiser<T> serialiser, @Nonnull TurtleUpgradeModeller<T> modeller )
    {
        TurtleUpgradeModellers.register( serialiser, modeller );
    }
}
