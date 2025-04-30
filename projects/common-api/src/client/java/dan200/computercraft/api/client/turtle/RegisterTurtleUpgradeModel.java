// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeType;

/**
 * A functional interface to register a {@link TurtleUpgradeModel} for a class of turtle upgrades.
 * <p>
 * This interface is largely intended to be used from multi-loader code, to allow sharing registration code between
 * multiple loaders.
 */
@FunctionalInterface
public interface RegisterTurtleUpgradeModel {
    /**
     * Register a {@link TurtleUpgradeModel}.
     *
     * @param type The turtle upgrade type.
     * @param mode The unbaked upgrade model.
     * @param <T>  The type of the turtle upgrade.
     */
    <T extends ITurtleUpgrade> void register(UpgradeType<T> type, TurtleUpgradeModel.Unbaked<? super T> mode);
}
