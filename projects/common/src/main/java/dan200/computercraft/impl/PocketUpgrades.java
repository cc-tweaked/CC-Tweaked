// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.IPocketUpgrade;

import java.util.stream.Stream;

public final class PocketUpgrades {
    private static final UpgradeManager<IPocketUpgrade> registry = new UpgradeManager<>(
        "pocket computer upgrade", "computercraft/pocket_upgrades", IPocketUpgrade.serialiserRegistryKey()
    );

    private PocketUpgrades() {
    }

    public static UpgradeManager<IPocketUpgrade> instance() {
        return registry;
    }

    public static Stream<IPocketUpgrade> getVanillaUpgrades() {
        return instance().getUpgradeWrappers().values().stream()
            .filter(x -> x.modId().equals(ComputerCraftAPI.MOD_ID))
            .map(UpgradeManager.UpgradeWrapper::upgrade);
    }
}
