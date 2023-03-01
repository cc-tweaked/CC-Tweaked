// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ComputerCraftTags.Blocks;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

import static dan200.computercraft.shared.ModRegistry.Items;
import static dan200.computercraft.shared.ModRegistry.TurtleSerialisers;

class TurtleUpgradeProvider extends TurtleUpgradeDataProvider {
    TurtleUpgradeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addUpgrades(Consumer<Upgrade<TurtleUpgradeSerialiser<?>>> addUpgrade) {
        simpleWithCustomItem(id("speaker"), TurtleSerialisers.SPEAKER.get(), Items.SPEAKER.get()).add(addUpgrade);
        simpleWithCustomItem(vanilla("crafting_table"), TurtleSerialisers.WORKBENCH.get(), net.minecraft.world.item.Items.CRAFTING_TABLE).add(addUpgrade);
        simpleWithCustomItem(id("wireless_modem_normal"), TurtleSerialisers.WIRELESS_MODEM_NORMAL.get(), Items.WIRELESS_MODEM_NORMAL.get()).add(addUpgrade);
        simpleWithCustomItem(id("wireless_modem_advanced"), TurtleSerialisers.WIRELESS_MODEM_ADVANCED.get(), Items.WIRELESS_MODEM_ADVANCED.get()).add(addUpgrade);

        tool(vanilla("diamond_axe"), net.minecraft.world.item.Items.DIAMOND_AXE).damageMultiplier(6.0f).add(addUpgrade);
        tool(vanilla("diamond_pickaxe"), net.minecraft.world.item.Items.DIAMOND_PICKAXE).add(addUpgrade);
        tool(vanilla("diamond_hoe"), net.minecraft.world.item.Items.DIAMOND_HOE).breakable(Blocks.TURTLE_HOE_BREAKABLE).add(addUpgrade);
        tool(vanilla("diamond_shovel"), net.minecraft.world.item.Items.DIAMOND_SHOVEL).breakable(Blocks.TURTLE_SHOVEL_BREAKABLE).add(addUpgrade);
        tool(vanilla("diamond_sword"), net.minecraft.world.item.Items.DIAMOND_SWORD).breakable(Blocks.TURTLE_SWORD_BREAKABLE).damageMultiplier(9.0f).add(addUpgrade);
    }

    private static ResourceLocation id(String id) {
        return new ResourceLocation(ComputerCraftAPI.MOD_ID, id);
    }

    private static ResourceLocation vanilla(String id) {
        // Naughty, please don't do this. Mostly here for some semblance of backwards compatibility.
        return new ResourceLocation("minecraft", id);
    }
}
