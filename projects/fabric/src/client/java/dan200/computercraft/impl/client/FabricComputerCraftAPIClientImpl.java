// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.client;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;

@AutoService(FabricComputerCraftAPIClientService.class)
public final class FabricComputerCraftAPIClientImpl implements FabricComputerCraftAPIClientService {
    @Override
    public <T extends ITurtleUpgrade> void registerTurtleUpgradeModeller(UpgradeType<T> type, TurtleUpgradeModeller<T> modeller) {
        TurtleUpgradeModellers.register(type, modeller);
    }
}
