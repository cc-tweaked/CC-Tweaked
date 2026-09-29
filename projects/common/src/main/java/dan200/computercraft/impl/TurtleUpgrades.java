// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;

public final class TurtleUpgrades {
    private static final UpgradeManager<TurtleUpgradeSerialiser<?>, ITurtleUpgrade> registry = new UpgradeManager<>(
        "turtle upgrade", "computercraft/turtle_upgrades", TurtleUpgradeSerialiser.registryId()
    );

    private TurtleUpgrades() {
    }

    public static UpgradeManager<TurtleUpgradeSerialiser<?>, ITurtleUpgrade> instance() {
        return registry;
    }
}
