// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;
import dan200.computercraft.impl.client.ComputerCraftAPIClientService;

@AutoService(ComputerCraftAPIClientService.class)
public final class ComputerCraftAPIClientImpl implements ComputerCraftAPIClientService {
    @Override
    public <T extends ITurtleUpgrade> void registerTurtleUpgradeModeller(TurtleUpgradeSerialiser<T> serialiser, TurtleUpgradeModeller<T> modeller) {
        TurtleUpgradeModellers.register(serialiser, modeller);
    }
}
