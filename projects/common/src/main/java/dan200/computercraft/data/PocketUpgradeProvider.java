// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketUpgradeDataProvider;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

import static dan200.computercraft.shared.ModRegistry.Items;

class PocketUpgradeProvider extends PocketUpgradeDataProvider {
    PocketUpgradeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addUpgrades(Consumer<Upgrade<IPocketUpgrade>> addUpgrade) {
        upgrade(id("speaker"), new PocketSpeaker(new ItemStack(Items.SPEAKER.get()))).add(addUpgrade);
        upgrade(id("wireless_modem_normal"), new PocketModem(new ItemStack(Items.WIRELESS_MODEM_NORMAL.get()), false)).add(addUpgrade);
        upgrade(id("wireless_modem_advanced"), new PocketModem(new ItemStack(Items.WIRELESS_MODEM_ADVANCED.get()), true)).add(addUpgrade);
    }

    private static ResourceLocation id(String id) {
        return new ResourceLocation(ComputerCraftAPI.MOD_ID, id);
    }
}
