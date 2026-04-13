// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.client.turtle.BasicUpgradeModel;
import dan200.computercraft.api.client.turtle.ItemUpgradeModel;
import dan200.computercraft.api.client.turtle.SelectUpgradeModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.turtle.upgrades.TurtleCraftingTable;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import dan200.computercraft.shared.turtle.upgrades.TurtleSpeaker;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import java.util.function.BiConsumer;

import static dan200.computercraft.api.turtle.TurtleToolBuilder.tool;

class TurtleUpgradeProvider {
    private static final ResourceKey<ITurtleUpgrade> SPEAKER = id("speaker");
    private static final ResourceKey<ITurtleUpgrade> CRAFTING_TABLE = vanilla("crafting_table");
    private static final ResourceKey<ITurtleUpgrade> WIRELESS_MODEM_NORMAL = id("wireless_modem_normal");
    private static final ResourceKey<ITurtleUpgrade> WIRELESS_MODEM_ADVANCED = id("wireless_modem_advanced");

    private static final ResourceKey<ITurtleUpgrade> DIAMOND_AXE = vanilla("diamond_axe");
    private static final ResourceKey<ITurtleUpgrade> DIAMOND_PICKAXE = vanilla("diamond_pickaxe");
    private static final ResourceKey<ITurtleUpgrade> DIAMOND_HOE = vanilla("diamond_hoe");
    private static final ResourceKey<ITurtleUpgrade> DIAMOND_SHOVEL = vanilla("diamond_shovel");
    private static final ResourceKey<ITurtleUpgrade> DIAMOND_SWORD = vanilla("diamond_sword");

    private static ResourceKey<ITurtleUpgrade> id(String id) {
        return ITurtleUpgrade.createKey(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, id));
    }

    private static ResourceKey<ITurtleUpgrade> vanilla(String id) {
        // Naughty, please don't do this. Mostly here for some semblance of backwards compatibility.
        return ITurtleUpgrade.createKey(Identifier.fromNamespaceAndPath("minecraft", id));
    }

    public static void register(BootstrapContext<ITurtleUpgrade> upgrades) {
        upgrades.register(SPEAKER, new TurtleSpeaker(new ItemStackTemplate(ModRegistry.Items.SPEAKER.get())));
        upgrades.register(CRAFTING_TABLE, new TurtleCraftingTable(new ItemStackTemplate(Items.CRAFTING_TABLE)));
        upgrades.register(WIRELESS_MODEM_NORMAL, new TurtleModem(new ItemStackTemplate(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get()), false));
        upgrades.register(WIRELESS_MODEM_ADVANCED, new TurtleModem(new ItemStackTemplate(ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get()), true));

        tool(DIAMOND_AXE, Items.DIAMOND_AXE).damageMultiplier(6.0f).register(upgrades);
        tool(DIAMOND_PICKAXE, Items.DIAMOND_PICKAXE).register(upgrades);
        tool(DIAMOND_HOE, Items.DIAMOND_HOE).breakable(ComputerCraftTags.Blocks.TURTLE_HOE_BREAKABLE).register(upgrades);
        tool(DIAMOND_SHOVEL, Items.DIAMOND_SHOVEL).breakable(ComputerCraftTags.Blocks.TURTLE_SHOVEL_BREAKABLE).register(upgrades);
        tool(DIAMOND_SWORD, Items.DIAMOND_SWORD).breakable(ComputerCraftTags.Blocks.TURTLE_SWORD_BREAKABLE).damageMultiplier(9.0f).register(upgrades);
    }

    public static void addModels(BiConsumer<Identifier, TurtleUpgradeModel.Unbaked> out) {
        out.accept(SPEAKER.identifier(), BasicUpgradeModel.unbaked(
            Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_speaker_left"),
            Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_speaker_right")
        ));
        out.accept(CRAFTING_TABLE.identifier(), BasicUpgradeModel.unbaked(
            Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_crafting_table_left"),
            Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_crafting_table_right")
        ));

        out.accept(WIRELESS_MODEM_NORMAL.identifier(), createModemModel("normal"));
        out.accept(WIRELESS_MODEM_ADVANCED.identifier(), createModemModel("advanced"));

        out.accept(DIAMOND_AXE.identifier(), ItemUpgradeModel.unbaked());
        out.accept(DIAMOND_PICKAXE.identifier(), ItemUpgradeModel.unbaked());
        out.accept(DIAMOND_HOE.identifier(), ItemUpgradeModel.unbaked());
        out.accept(DIAMOND_SHOVEL.identifier(), ItemUpgradeModel.unbaked());
        out.accept(DIAMOND_SWORD.identifier(), ItemUpgradeModel.unbaked());
    }

    private static TurtleUpgradeModel.Unbaked createModemModel(String type) {
        return SelectUpgradeModel.onComponent(ModRegistry.DataComponents.ON.get())
            .when(false, createBaseModemModel(type, "off"))
            .when(true, createBaseModemModel(type, "on"))
            .fallback(createBaseModemModel(type, "off"))
            .create();
    }

    private static TurtleUpgradeModel.Unbaked createBaseModemModel(String type, String state) {
        return BasicUpgradeModel.unbaked(
            Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_" + state + "_left"),
            Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_" + state + "_right")
        );
    }
}
