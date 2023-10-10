// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.PocketUpgradeDataProvider;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

import static dan200.computercraft.shared.ModRegistry.Items;
import static dan200.computercraft.shared.ModRegistry.PocketUpgradeSerialisers;

class PocketUpgradeProvider extends PocketUpgradeDataProvider {
    PocketUpgradeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addUpgrades(Consumer<Upgrade<PocketUpgradeSerialiser<?>>> addUpgrade) {
        addUpgrade.accept(simpleWithCustomItem(id("speaker"), PocketUpgradeSerialisers.SPEAKER.get(), Items.SPEAKER.get()));
        simpleWithCustomItem(id("wireless_modem_normal"), PocketUpgradeSerialisers.WIRELESS_MODEM_NORMAL.get(), Items.WIRELESS_MODEM_NORMAL.get()).add(addUpgrade);
        simpleWithCustomItem(id("wireless_modem_advanced"), PocketUpgradeSerialisers.WIRELESS_MODEM_ADVANCED.get(), Items.WIRELESS_MODEM_ADVANCED.get()).add(addUpgrade);
    }

    private static ResourceLocation id(String id) {
        return new ResourceLocation(ComputerCraftAPI.MOD_ID, id);
    }
}
