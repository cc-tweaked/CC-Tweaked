// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;

public final class PocketUpgrades {
    private static final UpgradeManager<PocketUpgradeSerialiser<?>, IPocketUpgrade> registry = new UpgradeManager<>(
        "pocket computer upgrade", "computercraft/pocket_upgrades", PocketUpgradeSerialiser.registryId()
    );

    private PocketUpgrades() {
    }

    public static UpgradeManager<PocketUpgradeSerialiser<?>, IPocketUpgrade> instance() {
        return registry;
    }
}
