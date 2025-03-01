// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.pocket.IPocketUpgrade;

public final class PocketUpgrades {
    private static final UpgradeManager<IPocketUpgrade> registry = new UpgradeManager<>(
        IPocketUpgrade.typeRegistry(), IPocketUpgrade.REGISTRY, IPocketUpgrade::getType
    );

    private PocketUpgrades() {
    }

    public static UpgradeManager<IPocketUpgrade> instance() {
        return registry;
    }
}
