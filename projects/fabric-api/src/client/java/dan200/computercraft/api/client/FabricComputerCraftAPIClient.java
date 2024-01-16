// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client;

import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.impl.client.ComputerCraftAPIClientService;

/**
 * The Fabric-specific entrypoint for ComputerCraft's API.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI The main API
 * @see dan200.computercraft.api.client.ComputerCraftAPIClient The main client-side API
 */
public final class FabricComputerCraftAPIClient {
    private FabricComputerCraftAPIClient() {
    }

    /**
     * Register a {@link TurtleUpgradeModeller} for a class of turtle upgrades.
     * <p>
     * This may be called at any point after registry creation, though it is recommended to call it within your client
     * setup step.
     * <p>
     * This method may be used as a {@link dan200.computercraft.api.client.turtle.RegisterTurtleUpgradeModeller}, for
     * convenient use in multi-loader code.
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
