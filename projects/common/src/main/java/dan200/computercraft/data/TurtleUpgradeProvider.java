// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.turtle.upgrades.TurtleCraftingTable;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import dan200.computercraft.shared.turtle.upgrades.TurtleSpeaker;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static dan200.computercraft.api.turtle.TurtleToolBuilder.tool;

class TurtleUpgradeProvider {
    public static void addUpgrades(BootstrapContext<ITurtleUpgrade> upgrades) {
        upgrades.register(id("speaker"), new TurtleSpeaker(new ItemStack(ModRegistry.Items.SPEAKER.get())));
        upgrades.register(vanilla("crafting_table"), new TurtleCraftingTable(new ItemStack(Items.CRAFTING_TABLE)));
        upgrades.register(id("wireless_modem_normal"), new TurtleModem(new ItemStack(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get()), false));
        upgrades.register(id("wireless_modem_advanced"), new TurtleModem(new ItemStack(ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get()), true));

        tool(vanilla("diamond_axe").location(), Items.DIAMOND_AXE).damageMultiplier(6.0f).register(upgrades);
        tool(vanilla("diamond_pickaxe"), Items.DIAMOND_PICKAXE).register(upgrades);
        tool(vanilla("diamond_hoe"), Items.DIAMOND_HOE).breakable(ComputerCraftTags.Blocks.TURTLE_HOE_BREAKABLE).register(upgrades);
        tool(vanilla("diamond_shovel"), Items.DIAMOND_SHOVEL).breakable(ComputerCraftTags.Blocks.TURTLE_SHOVEL_BREAKABLE).register(upgrades);
        tool(vanilla("diamond_sword"), Items.DIAMOND_SWORD).breakable(ComputerCraftTags.Blocks.TURTLE_SWORD_BREAKABLE).damageMultiplier(9.0f).register(upgrades);
    }

    private static ResourceKey<ITurtleUpgrade> id(String id) {
        return ITurtleUpgrade.createKey(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, id));
    }

    private static ResourceKey<ITurtleUpgrade> vanilla(String id) {
        // Naughty, please don't do this. Mostly here for some semblance of backwards compatibility.
        return ITurtleUpgrade.createKey(ResourceLocation.fromNamespaceAndPath("minecraft", id));
    }
}
