// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.google.gson.JsonObject;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.pocket.PocketUpgradeDataProvider;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.command.arguments.ComputerSelector;
import dan200.computercraft.shared.computer.metrics.basic.Aggregate;
import dan200.computercraft.shared.computer.metrics.basic.AggregatedMetric;
import dan200.computercraft.shared.config.ConfigFile;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class LanguageProvider implements DataProvider {
    private final PackOutput output;
    private final TurtleUpgradeDataProvider turtleUpgrades;
    private final PocketUpgradeDataProvider pocketUpgrades;

    private final Map<String, String> translations = new HashMap<>();

    public LanguageProvider(PackOutput output, TurtleUpgradeDataProvider turtleUpgrades, PocketUpgradeDataProvider pocketUpgrades) {
        this.output = output;
        this.turtleUpgrades = turtleUpgrades;
        this.pocketUpgrades = pocketUpgrades;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        addTranslations();
        getExpectedKeys().forEach(x -> {
            if (!translations.containsKey(x)) throw new IllegalStateException("No translation for " + x);
        });

        var json = new JsonObject();
        for (var pair : translations.entrySet()) json.addProperty(pair.getKey(), pair.getValue());
        return DataProvider.saveStable(cachedOutput, json, output.getOutputFolder().resolve("assets/" + ComputerCraftAPI.MOD_ID + "/lang/en_us.json"));
    }

    @Override
    public String getName() {
        return "Languages";
    }

    private void addTranslations() {
        add("itemGroup.computercraft", "ComputerCraft");

        // Blocks and items
        add(ModRegistry.Items.COMPUTER_NORMAL.get(), "Computer");
        add(ModRegistry.Items.COMPUTER_ADVANCED.get(), "Advanced Computer");
        add(ModRegistry.Items.COMPUTER_COMMAND.get(), "Command Computer");

        add(ModRegistry.Items.DISK_DRIVE.get(), "Disk Drive");
        add(ModRegistry.Items.PRINTER.get(), "Printer");
        add(ModRegistry.Items.SPEAKER.get(), "Speaker");
        add(ModRegistry.Items.MONITOR_NORMAL.get(), "Monitor");
        add(ModRegistry.Items.MONITOR_ADVANCED.get(), "Advanced Monitor");
        add(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get(), "Wireless Modem");
        add(ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get(), "Ender Modem");
        add(ModRegistry.Items.WIRED_MODEM.get(), "Wired Modem");
        add(ModRegistry.Items.CABLE.get(), "Networking Cable");
        add(ModRegistry.Items.WIRED_MODEM_FULL.get(), "Wired Modem");
        add(ModRegistry.Items.REDSTONE_RELAY.get(), "Redstone Relay");

        add(ModRegistry.Items.TURTLE_NORMAL.get(), "Turtle");
        add(ModRegistry.Blocks.TURTLE_NORMAL.get().getDescriptionId() + ".upgraded", "%s Turtle");
        add(ModRegistry.Blocks.TURTLE_NORMAL.get().getDescriptionId() + ".upgraded_twice", "%s %s Turtle");

        add(ModRegistry.Items.TURTLE_ADVANCED.get(), "Advanced Turtle");
        add(ModRegistry.Blocks.TURTLE_ADVANCED.get().getDescriptionId() + ".upgraded", "Advanced %s Turtle");
        add(ModRegistry.Blocks.TURTLE_ADVANCED.get().getDescriptionId() + ".upgraded_twice", "Advanced %s %s Turtle");

        add(ModRegistry.Items.DISK.get(), "Floppy Disk");
        add(ModRegistry.Items.TREASURE_DISK.get(), "Floppy Disk");
        add(ModRegistry.Items.PRINTED_PAGE.get(), "Printed Page");
        add(ModRegistry.Items.PRINTED_PAGES.get(), "Printed Pages");
        add(ModRegistry.Items.PRINTED_BOOK.get(), "Printed Book");

        add(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), "Pocket Computer");
        add(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get().getDescriptionId() + ".upgraded", "%s Pocket Computer");
        add(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get(), "Advanced Pocket Computer");
        add(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get().getDescriptionId() + ".upgraded", "Advanced %s Pocket Computer");

        // Tags (for EMI)
        add(ComputerCraftTags.Items.COMPUTER, "Computers");
        add(ComputerCraftTags.Items.TURTLE, "Turtles");
        add(ComputerCraftTags.Items.WIRED_MODEM, "Wired modems");
        add(ComputerCraftTags.Items.MONITOR, "Monitors");

        // Turtle/pocket upgrades
        add("upgrade.minecraft.diamond_sword.adjective", "Melee");
        add("upgrade.minecraft.diamond_shovel.adjective", "Digging");
        add("upgrade.minecraft.diamond_pickaxe.adjective", "Mining");
        add("upgrade.minecraft.diamond_axe.adjective", "Felling");
        add("upgrade.minecraft.diamond_hoe.adjective", "Farming");
        add("upgrade.minecraft.crafting_table.adjective", "Crafty");
        add("upgrade.computercraft.wireless_modem_normal.adjective", "Wireless");
        add("upgrade.computercraft.wireless_modem_advanced.adjective", "Ender");
        add("upgrade.computercraft.speaker.adjective", "Noisy");

        add("chat.computercraft.wired_modem.peripheral_connected", "Peripheral \"%s\" connected to network");
        add("chat.computercraft.wired_modem.peripheral_disconnected", "Peripheral \"%s\" disconnected from network");

        // Commands
        add("commands.computercraft.synopsis", "Various commands for controlling computers.");
        add("commands.computercraft.desc", "The /computercraft command provides various debugging and administrator tools for controlling and interacting with computers.");
        add("commands.computercraft.help.synopsis", "Provide help for a specific command");
        add("commands.computercraft.help.desc", "Displays this help message");
        add("commands.computercraft.help.no_children", "%s has no sub-commands");
        add("commands.computercraft.help.no_command", "No such command '%s'");
        add("commands.computercraft.dump.synopsis", "Display the status of computers.");
        add("commands.computercraft.dump.desc", "Display the status of all computers or specific information about one computer. You can specify the computer's instance id (e.g. 123), computer id (e.g #123) or label (e.g. \"@My Computer\").");
        add("commands.computercraft.dump.action", "View more info about this computer");
        add("commands.computercraft.dump.open_path", "View this computer's files");
        add("commands.computercraft.shutdown.synopsis", "Shutdown computers remotely.");
        add("commands.computercraft.shutdown.desc", "Shutdown the listed computers or all if none are specified. You can specify the computer's instance id (e.g. 123), computer id (e.g #123) or label (e.g. \"@My Computer\").");
        add("commands.computercraft.shutdown.done", "Shutdown %s/%s computers");
        add("commands.computercraft.turn_on.synopsis", "Turn computers on remotely.");
        add("commands.computercraft.turn_on.desc", "Turn on the listed computers. You can specify the computer's instance id (e.g. 123), computer id (e.g #123) or label (e.g. \"@My Computer\").");
        add("commands.computercraft.turn_on.done", "Turned on %s/%s computers");
        add("commands.computercraft.tp.synopsis", "Teleport to a specific computer.");
        add("commands.computercraft.tp.desc", "Teleport to the location of a computer. You can either specify the computer's instance id (e.g. 123) or computer id (e.g #123).");
        add("commands.computercraft.tp.action", "Teleport to this computer");
        add("commands.computercraft.view.synopsis", "View the terminal of a computer.");
        add("commands.computercraft.view.desc", "Open the terminal of a computer, allowing remote control of a computer. This does not provide access to turtle's inventories. You can either specify the computer's instance id (e.g. 123) or computer id (e.g #123).");
        add("commands.computercraft.view.action", "View this computer");
        add("commands.computercraft.view.not_player", "Cannot open terminal for non-player");
        add("commands.computercraft.track.synopsis", "Track execution times for computers.");
        add("commands.computercraft.track.desc", "Track how long computers execute for, as well as how many events they handle. This presents information in a similar way to /forge track and can be useful for diagnosing lag.");
        add("commands.computercraft.track.start.synopsis", "Start tracking all computers");
        add("commands.computercraft.track.start.desc", "Start tracking all computers' execution times and event counts. This will discard the results of previous runs.");
        add("commands.computercraft.track.start.stop", "Run %s to stop tracking and view the results");
        add("commands.computercraft.track.stop.synopsis", "Stop tracking all computers");
        add("commands.computercraft.track.stop.desc", "Stop tracking all computers' events and execution times");
        add("commands.computercraft.track.stop.action", "Click to stop tracking");
        add("commands.computercraft.track.stop.not_enabled", "Not currently tracking computers");
        add("commands.computercraft.track.dump.synopsis", "Dump the latest track results");
        add("commands.computercraft.track.dump.desc", "Dump the latest results of computer tracking.");
        add("commands.computercraft.track.dump.no_timings", "No timings available");
        add("commands.computercraft.track.dump.computer", "Computer");
        add("commands.computercraft.queue.synopsis", "Send a computer_command event to a command computer");
        add("commands.computercraft.queue.desc", "Send a computer_command event to a command computer, passing through the additional arguments. This is mostly designed for map makers, acting as a more computer-friendly version of /trigger. Any player can run the command, which would most likely be done through a text component's click event.");

        add("commands.computercraft.generic.yes", "Y");
        add("commands.computercraft.generic.no", "N");
        add("commands.computercraft.generic.exception", "Unhandled exception (%s)");
        add("commands.computercraft.generic.additional_rows", "%d additional rows…");

        // Argument types
        add("argument.computercraft.computer.instance", "Unique instance ID");
        add("argument.computercraft.computer.id", "Computer ID");
        add("argument.computercraft.computer.label", "Computer label");
        add("argument.computercraft.computer.distance", "Distance to entity");
        add("argument.computercraft.computer.family", "Computer family");

        // Exceptions
        add("argument.computercraft.computer.no_matching", "No computers matching '%s'");
        add("argument.computercraft.computer.many_matching", "Multiple computers matching '%s' (instances %s)");
        add("argument.computercraft.tracking_field.no_field", "Unknown field '%s'");
        add("argument.computercraft.argument_expected", "Argument expected");
        add("argument.computercraft.unknown_computer_family", "Unknown computer family '%s'");

        // Metrics
        add(Metrics.COMPUTER_TASKS, "Tasks");
        add(Metrics.SERVER_TASKS, "Server tasks");
        add(Metrics.JAVA_ALLOCATION, "Java Allocations");
        add(Metrics.PERIPHERAL_OPS, "Peripheral calls");
        add(Metrics.FS_OPS, "Filesystem operations");
        add(Metrics.HTTP_REQUESTS, "HTTP requests");
        add(Metrics.HTTP_UPLOAD, "HTTP upload");
        add(Metrics.HTTP_DOWNLOAD, "HTTP download");
        add(Metrics.WEBSOCKET_INCOMING, "Websocket incoming");
        add(Metrics.WEBSOCKET_OUTGOING, "Websocket outgoing");
        add(Metrics.TURTLE_OPS, "Turtle operations");

        add(AggregatedMetric.TRANSLATION_PREFIX + Aggregate.MAX.id(), "%s (max)");
        add(AggregatedMetric.TRANSLATION_PREFIX + Aggregate.AVG.id(), "%s (avg)");
        add(AggregatedMetric.TRANSLATION_PREFIX + Aggregate.COUNT.id(), "%s (count)");

        // Additional UI elements
        add("gui.computercraft.terminal", "Computer terminal");
        add("gui.computercraft.tooltip.copy", "Copy to clipboard");
        add("gui.computercraft.tooltip.computer_id", "Computer ID: %s");
        add("gui.computercraft.tooltip.disk_id", "Disk ID: %s");
        add("gui.computercraft.tooltip.turn_on", "Turn this computer on");
        add("gui.computercraft.tooltip.turn_off", "Turn this computer off");
        add("gui.computercraft.tooltip.turn_off.key", "Hold Ctrl+S");
        add("gui.computercraft.tooltip.terminate", "Stop the currently running code");
        add("gui.computercraft.tooltip.terminate.key", "Hold Ctrl+T");
        add("gui.computercraft.upload.failed", "Upload Failed");
        add("gui.computercraft.upload.failed.computer_off", "You must turn the computer on before uploading files.");
        add("gui.computercraft.upload.failed.too_much", "Your files are too large to be uploaded.");
        add("gui.computercraft.upload.failed.name_too_long", "File names are too long to be uploaded.");
        add("gui.computercraft.upload.failed.too_many_files", "Cannot upload this many files.");
        add("gui.computercraft.upload.failed.generic", "Uploading files failed (%s)");
        add("gui.computercraft.upload.failed.corrupted", "Files corrupted when uploading. Please try again.");
        add("gui.computercraft.upload.no_response", "Transferring Files");
        add("gui.computercraft.upload.no_response.msg", "Your computer has not used your transferred files. You may need to run the %s program and try again.");
        add("gui.computercraft.pocket_computer_overlay", "Pocket computer open. Press ESC to close.");

        // Config options
        addConfigEntry(ConfigSpec.computerSpaceLimit, "Computer space limit (bytes)");
        addConfigEntry(ConfigSpec.floppySpaceLimit, "Floppy Disk space limit (bytes)");
        addConfigEntry(ConfigSpec.uploadMaxSize, "File upload size limit (bytes)");
        addConfigEntry(ConfigSpec.maximumFilesOpen, "Maximum files open per computer");
        addConfigEntry(ConfigSpec.defaultComputerSettings, "Default Computer settings");
        addConfigEntry(ConfigSpec.logComputerErrors, "Log computer errors");
        addConfigEntry(ConfigSpec.commandRequireCreative, "Command computers require creative");
        addConfigEntry(ConfigSpec.disabledGenericMethods, "Disabled generic methods");

        addConfigGroup(ConfigSpec.serverSpec, "execution", "Execution");
        addConfigEntry(ConfigSpec.computerThreads, "Computer threads");
        addConfigEntry(ConfigSpec.maxMainGlobalTime, "Server tick global time limit");
        addConfigEntry(ConfigSpec.maxMainComputerTime, "Server tick computer time limit");

        addConfigGroup(ConfigSpec.serverSpec, "http", "HTTP");
        addConfigEntry(ConfigSpec.httpEnabled, "Enable the HTTP API");
        addConfigEntry(ConfigSpec.httpWebsocketEnabled, "Enable websockets");
        addConfigEntry(ConfigSpec.httpRules, "Allow/deny rules");
        addConfigEntry(ConfigSpec.httpMaxRequests, "Maximum concurrent requests");
        addConfigEntry(ConfigSpec.httpMaxWebsockets, "Maximum concurrent websockets");
        addConfigGroup(ConfigSpec.serverSpec, "http.bandwidth", "Bandwidth");
        addConfigEntry(ConfigSpec.httpDownloadBandwidth, "Global download limit");
        addConfigEntry(ConfigSpec.httpUploadBandwidth, "Global upload limit");

        addConfigGroup(ConfigSpec.serverSpec, "http.proxy", "Proxy");
        addConfigEntry(ConfigSpec.httpProxyHost, "Host name");
        addConfigEntry(ConfigSpec.httpProxyPort, "Port");
        addConfigEntry(ConfigSpec.httpProxyType, "Proxy type");

        addConfigGroup(ConfigSpec.serverSpec, "peripheral", "Peripherals");
        addConfigEntry(ConfigSpec.commandBlockEnabled, "Enable command block peripheral");
        addConfigEntry(ConfigSpec.modemRange, "Modem range (default)");
        addConfigEntry(ConfigSpec.modemHighAltitudeRange, "Modem range (high-altitude)");
        addConfigEntry(ConfigSpec.modemRangeDuringStorm, "Modem range (bad weather)");
        addConfigEntry(ConfigSpec.modemHighAltitudeRangeDuringStorm, "Modem range (high-altitude, bad weather)");
        addConfigEntry(ConfigSpec.maxNotesPerTick, "Maximum notes that a computer can play at once");
        addConfigEntry(ConfigSpec.monitorBandwidth, "Monitor bandwidth");

        addConfigGroup(ConfigSpec.serverSpec, "turtle", "Turtles");
        addConfigEntry(ConfigSpec.turtlesNeedFuel, "Enable fuel");
        addConfigEntry(ConfigSpec.turtleFuelLimit, "Turtle fuel limit");
        addConfigEntry(ConfigSpec.advancedTurtleFuelLimit, "Advanced Turtle fuel limit");
        addConfigEntry(ConfigSpec.turtlesCanPush, "Turtles can push entities");

        addConfigGroup(ConfigSpec.serverSpec, "term_sizes", "Terminal sizes");
        addConfigGroup(ConfigSpec.serverSpec, "term_sizes.computer", "Computer");
        addConfigEntry(ConfigSpec.computerTermWidth, "Terminal width");
        addConfigEntry(ConfigSpec.computerTermHeight, "Terminal height");
        addConfigGroup(ConfigSpec.serverSpec, "term_sizes.pocket_computer", "Pocket Computer");
        addConfigEntry(ConfigSpec.pocketTermWidth, "Terminal width");
        addConfigEntry(ConfigSpec.pocketTermHeight, "Terminal height");
        addConfigGroup(ConfigSpec.serverSpec, "term_sizes.monitor", "Monitor");
        addConfigEntry(ConfigSpec.monitorWidth, "Max monitor width");
        addConfigEntry(ConfigSpec.monitorHeight, "Max monitor height");

        addConfigEntry(ConfigSpec.monitorRenderer, "Monitor renderer");
        addConfigEntry(ConfigSpec.monitorDistance, "Monitor distance");
        addConfigEntry(ConfigSpec.uploadNagDelay, "Upload nag delay");
    }

    private Stream<String> getExpectedKeys() {
        return Stream.of(
            RegistryWrappers.BLOCKS.stream()
                .filter(x -> RegistryWrappers.BLOCKS.getKey(x).getNamespace().equals(ComputerCraftAPI.MOD_ID))
                .map(Block::getDescriptionId)
                // Exclude blocks that just reuse vanilla translations, such as the lectern.
                .filter(x -> !x.startsWith("block.minecraft.")),
            RegistryWrappers.ITEMS.stream()
                .filter(x -> RegistryWrappers.ITEMS.getKey(x).getNamespace().equals(ComputerCraftAPI.MOD_ID))
                .map(Item::getDescriptionId),
            turtleUpgrades.getGeneratedUpgrades().stream().map(UpgradeBase::getUnlocalisedAdjective),
            pocketUpgrades.getGeneratedUpgrades().stream().map(UpgradeBase::getUnlocalisedAdjective),
            Metric.metrics().values().stream().map(x -> AggregatedMetric.TRANSLATION_PREFIX + x.name() + ".name"),
            ConfigSpec.serverSpec.entries().map(ConfigFile.Entry::translationKey),
            ConfigSpec.clientSpec.entries().map(ConfigFile.Entry::translationKey),
            ComputerSelector.options().values().stream().map(ComputerSelector.Option::translationKey)
        ).flatMap(x -> x);
    }

    private void add(String id, String text) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(text, "text cannot be null");

        if (translations.containsKey(id)) throw new IllegalArgumentException("Duplicate translation " + id);
        translations.put(id, text);
    }

    private void add(Item item, String text) {
        add(item.getDescriptionId(), text);
    }

    private void add(Metric metric, String text) {
        add(AggregatedMetric.TRANSLATION_PREFIX + metric.name() + ".name", text);
    }

    private void add(TagKey<Item> tag, String text) {
        add("tag.item." + tag.location().getNamespace() + "." + tag.location().getPath(), text);
    }

    private void addConfigGroup(ConfigFile spec, String path, String text) {
        var entry = spec.getEntry(path);
        if (!(entry instanceof ConfigFile.Group)) throw new IllegalArgumentException("Cannot find group " + path);
        addConfigEntry(entry, text);
    }

    private void addConfigEntry(ConfigFile.Entry value, String text) {
        add(value.translationKey(), text);
        add(value.translationKey() + ".tooltip", value.comment());
    }
}
