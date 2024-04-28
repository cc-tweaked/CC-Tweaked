// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client;

import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.impl.client.FabricComputerCraftAPIClientService;

/**
 * The Fabric-specific entrypoint for ComputerCraft's client-side API.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI The main API
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
     * @param type     The turtle upgrade type.
     * @param modeller The upgrade modeller.
     * @param <T>      The type of the turtle upgrade.
     */
    public static <T extends ITurtleUpgrade> void registerTurtleUpgradeModeller(UpgradeType<T> type, TurtleUpgradeModeller<T> modeller) {
        getInstance().registerTurtleUpgradeModeller(type, modeller);
    }

    private static FabricComputerCraftAPIClientService getInstance() {
        return FabricComputerCraftAPIClientService.get();
    }
}
