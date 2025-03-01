// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import static dan200.computercraft.shared.ModRegistry.Items;

class PocketUpgradeProvider {
    public static void addUpgrades(BootstrapContext<IPocketUpgrade> upgrades) {
        upgrades.register(id("speaker"), new PocketSpeaker(new ItemStack(Items.SPEAKER.get())));
        upgrades.register(id("wireless_modem_normal"), new PocketModem(new ItemStack(Items.WIRELESS_MODEM_NORMAL.get()), false));
        upgrades.register(id("wireless_modem_advanced"), new PocketModem(new ItemStack(Items.WIRELESS_MODEM_ADVANCED.get()), true));
    }

    private static ResourceKey<IPocketUpgrade> id(String id) {
        return ResourceKey.create(IPocketUpgrade.REGISTRY, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, id));
    }
}
