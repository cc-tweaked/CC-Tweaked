/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft;

import static dan200.computercraft.shared.ComputerCraftRegistry.ModBlocks;
import static dan200.computercraft.shared.ComputerCraftRegistry.init;

import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.core.apis.http.websocket.Websocket;
import dan200.computercraft.core.asm.GenericSource;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.computer.core.ClientComputerRegistry;
import dan200.computercraft.shared.computer.core.ServerComputerRegistry;
import dan200.computercraft.shared.computer.recipe.ComputerUpgradeRecipe;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.proxy.ComputerCraftProxyCommon;
import dan200.computercraft.shared.turtle.recipes.TurtleRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.turtle.upgrades.TurtleAxe;
import dan200.computercraft.shared.turtle.upgrades.TurtleCraftingTable;
import dan200.computercraft.shared.turtle.upgrades.TurtleHoe;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import dan200.computercraft.shared.turtle.upgrades.TurtleShovel;
import dan200.computercraft.shared.turtle.upgrades.TurtleSpeaker;
import dan200.computercraft.shared.turtle.upgrades.TurtleSword;
import dan200.computercraft.shared.turtle.upgrades.TurtleTool;
import dan200.computercraft.shared.util.Config;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import dan200.computercraft.shared.util.ServiceUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.loader.api.FabricLoader;

public final class ComputerCraft implements ModInitializer {
    public static final String MOD_ID = "computercraft";
    // Configuration options
    public static final String[] DEFAULT_HTTP_WHITELIST = new String[] {"*"};
    public static final String[] DEFAULT_HTTP_BLACKLIST = new String[] {
        "127.0.0.0/8",
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "fd00::/8",
        "0.0.0.0/8"
        };
    public static final int terminalWidth_computer = 51;
    public static final int terminalHeight_computer = 19;
    public static final int terminalWidth_turtle = 39;
    public static final int terminalHeight_turtle = 13;
    public static final int terminalWidth_pocketComputer = 26;
    public static final int terminalHeight_pocketComputer = 20;
    // Registries
    public static final ClientComputerRegistry clientComputerRegistry = new ClientComputerRegistry();
    public static final ServerComputerRegistry serverComputerRegistry = new ServerComputerRegistry();
    // Logging
    public static final Logger log = LogManager.getLogger(MOD_ID);
    public static ItemGroup MAIN_GROUP = FabricItemGroupBuilder.build(new Identifier(MOD_ID, "main"), () -> new ItemStack(ModBlocks.COMPUTER_NORMAL));
    public static boolean commandRequireCreative = false;
    public static MonitorRenderer monitorRenderer = MonitorRenderer.BEST;
    public static int computerSpaceLimit = 1000 * 1000;
    public static int floppySpaceLimit = 125 * 1000;
    public static int maximumFilesOpen = 128;
    public static boolean disable_lua51_features = false;
    public static String default_computer_settings = "";
    public static boolean debug_enable = true;
    public static boolean logPeripheralErrors = false;
    public static int computer_threads = 1;
    public static long maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos(10);
    public static long maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos(5);
    public static boolean http_enable = true;
    public static boolean http_websocket_enable = true;
    public static int httpTimeout = 30000;
    public static int httpMaxRequests = 16;
    public static long httpMaxDownload = 16 * 1024 * 1024;
    public static long httpMaxUpload = 4 * 1024 * 1024;
    public static int httpMaxWebsockets = 4;
    public static int httpMaxWebsocketMessage = Websocket.MAX_MESSAGE_SIZE;
    public static boolean enableCommandBlock = false;
    public static int modem_range = 64;
    public static int modem_highAltitudeRange = 384;
    public static int modem_rangeDuringStorm = 64;
    public static int modem_highAltitudeRangeDuringStorm = 384;
    public static int maxNotesPerTick = 8;
    public static boolean turtlesNeedFuel = true;
    public static int turtleFuelLimit = 20000;
    public static int advancedTurtleFuelLimit = 100000;
    public static boolean turtlesObeyBlockProtection = true;
    public static boolean turtlesCanPush = true;
    public static EnumSet<TurtleAction> turtleDisabledActions = EnumSet.noneOf(TurtleAction.class);
    public static int monitorWidth = 8;
    public static int monitorHeight = 6;
    public static double monitorDistanceSq = 4096;

    public static List<AddressRule> httpRules = buildHttpRulesFromConfig(DEFAULT_HTTP_BLACKLIST, DEFAULT_HTTP_WHITELIST);

    public static List<AddressRule> buildHttpRulesFromConfig(String[] blacklist, String[] whitelist) {
        return Stream.concat(Stream.of(blacklist)
                        .map( x -> AddressRule.parse( x, null, Action.DENY.toPartial()))
                        .filter(Objects::nonNull),
                Stream.of(whitelist)
                        .map( x -> AddressRule.parse( x, null, Action.ALLOW.toPartial()))
                        .filter(Objects::nonNull))
                .collect(Collectors.toList());
    }

    @Override
    public void onInitialize() {
        Config.load(Paths.get(FabricLoader.getInstance()
                                          .getConfigDir()
                                          .toFile()
                                          .getPath(), MOD_ID + ".json5"));
        ComputerCraftProxyCommon.init();
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(ComputerCraft.MOD_ID, "colour"), ColourableRecipe.SERIALIZER);
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(ComputerCraft.MOD_ID, "computer_upgrade"), ComputerUpgradeRecipe.SERIALIZER);
        Registry.register(Registry.RECIPE_SERIALIZER,
                          new Identifier(ComputerCraft.MOD_ID, "pocket_computer_upgrade"),
                          PocketComputerUpgradeRecipe.SERIALIZER);
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(ComputerCraft.MOD_ID, "disk"), DiskRecipe.SERIALIZER);
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(ComputerCraft.MOD_ID, "printout"), PrintoutRecipe.SERIALIZER);
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(ComputerCraft.MOD_ID, "turtle"), TurtleRecipe.SERIALIZER);
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(ComputerCraft.MOD_ID, "turtle_upgrade"), TurtleUpgradeRecipe.SERIALIZER);
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(ComputerCraft.MOD_ID, "impostor_shaped"), ImpostorRecipe.SERIALIZER);
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(ComputerCraft.MOD_ID, "impostor_shapeless"), ImpostorShapelessRecipe.SERIALIZER);
        Registry.register(Registry.LOOT_CONDITION_TYPE, new Identifier(ComputerCraft.MOD_ID, "block_named"), BlockNamedEntityLootCondition.TYPE);
        Registry.register(Registry.LOOT_CONDITION_TYPE, new Identifier(ComputerCraft.MOD_ID, "player_creative"), PlayerCreativeLootCondition.TYPE);
        Registry.register(Registry.LOOT_CONDITION_TYPE, new Identifier(ComputerCraft.MOD_ID, "has_id"), HasComputerIdLootCondition.TYPE);
        init();
        GenericSource.setup( () -> ServiceUtil.loadServices( GenericSource.class ));
    }

}
