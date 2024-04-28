// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ComputerCraftTags.Blocks;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.turtle.upgrades.TurtleCraftingTable;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import dan200.computercraft.shared.turtle.upgrades.TurtleSpeaker;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

class TurtleUpgradeProvider extends TurtleUpgradeDataProvider {
    TurtleUpgradeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addUpgrades(Consumer<Upgrade<ITurtleUpgrade>> addUpgrade) {
        upgrade(id("speaker"), new TurtleSpeaker(new ItemStack(ModRegistry.Items.SPEAKER.get()))).add(addUpgrade);
        upgrade(vanilla("crafting_table"), new TurtleCraftingTable(new ItemStack(Items.CRAFTING_TABLE))).add(addUpgrade);
        upgrade(id("wireless_modem_normal"), new TurtleModem(new ItemStack(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get()), false)).add(addUpgrade);
        upgrade(id("wireless_modem_advanced"), new TurtleModem(new ItemStack(ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get()), true)).add(addUpgrade);

        tool(vanilla("diamond_axe"), Items.DIAMOND_AXE).damageMultiplier(6.0f).add(addUpgrade);
        tool(vanilla("diamond_pickaxe"), Items.DIAMOND_PICKAXE).add(addUpgrade);
        tool(vanilla("diamond_hoe"), Items.DIAMOND_HOE).breakable(Blocks.TURTLE_HOE_BREAKABLE).add(addUpgrade);
        tool(vanilla("diamond_shovel"), Items.DIAMOND_SHOVEL).breakable(Blocks.TURTLE_SHOVEL_BREAKABLE).add(addUpgrade);
        tool(vanilla("diamond_sword"), Items.DIAMOND_SWORD).breakable(Blocks.TURTLE_SWORD_BREAKABLE).damageMultiplier(9.0f).add(addUpgrade);
    }

    private static ResourceLocation id(String id) {
        return new ResourceLocation(ComputerCraftAPI.MOD_ID, id);
    }

    private static ResourceLocation vanilla(String id) {
        // Naughty, please don't do this. Mostly here for some semblance of backwards compatibility.
        return new ResourceLocation("minecraft", id);
    }
}
