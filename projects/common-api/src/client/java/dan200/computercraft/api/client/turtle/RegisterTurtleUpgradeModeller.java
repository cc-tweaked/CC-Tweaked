// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;

/**
 * A functional interface to register a {@link TurtleUpgradeModeller} for a class of turtle upgrades.
 * <p>
 * This interface is largely intended to be used from multi-loader code, to allow sharing registration code between
 * multiple loaders.
 */
@FunctionalInterface
public interface RegisterTurtleUpgradeModeller {
    /**
     * Register a {@link TurtleUpgradeModeller}.
     *
     * @param serialiser The turtle upgrade serialiser.
     * @param modeller   The upgrade modeller.
     * @param <T>        The type of the turtle upgrade.
     */
    <T extends ITurtleUpgrade> void register(TurtleUpgradeSerialiser<T> serialiser, TurtleUpgradeModeller<T> modeller);
}
