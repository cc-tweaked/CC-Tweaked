/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.client;

import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.impl.client.ComputerCraftAPIClientService;


public final class ComputerCraftAPIClient {
    private ComputerCraftAPIClient() {
    }

    /**
     * Register a {@link TurtleUpgradeModeller} for a class of turtle upgrades.
     * <p>
     * This may be called at any point after registry creation, though it is recommended to call it within your client
     * setup step.
     *
     * @param serialiser The turtle upgrade serialiser.
     * @param modeller   The upgrade modeller.
     * @param <T>        The type of the turtle upgrade.
     */
    public static <T extends ITurtleUpgrade> void registerTurtleUpgradeModeller(TurtleUpgradeSerialiser<T> serialiser, TurtleUpgradeModeller<T> modeller) {
        getInstance().registerTurtleUpgradeModeller(serialiser, modeller);
    }

    private static ComputerCraftAPIClientService getInstance() {
        return ComputerCraftAPIClientService.get();
    }
}
