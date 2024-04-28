// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.ModRegistry;

public final class TurtleUpgrades {
    private static final UpgradeManager<ITurtleUpgrade> registry = new UpgradeManager<>(
        ITurtleUpgrade.typeRegistry(), ModRegistry.TURTLE_UPGRADE, ITurtleUpgrade::getType
    );

    private TurtleUpgrades() {
    }

    public static UpgradeManager<ITurtleUpgrade> instance() {
        return registry;
    }
}
