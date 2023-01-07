/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.core.CoreConfig;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.MarkerFilter;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class ConfigSpec {
    private static final int MODEM_MAX_RANGE = 100000;

    private static final String TRANSLATION_PREFIX = "gui.computercraft.config.";

    public static final ForgeConfigSpec serverSpec;

    public static final ConfigValue<Integer> computerSpaceLimit;
    public static final ConfigValue<Integer> floppySpaceLimit;
    public static final ConfigValue<Integer> maximumFilesOpen;
    public static final ConfigValue<Boolean> disableLua51Features;
    public static final ConfigValue<String> defaultComputerSettings;
    public static final ConfigValue<Boolean> logComputerErrors;
    public static final ConfigValue<Boolean> commandRequireCreative;

    public static final ConfigValue<Integer> computerThreads;
    public static final ConfigValue<Integer> maxMainGlobalTime;
    public static final ConfigValue<Integer> maxMainComputerTime;

    public static final ConfigValue<Boolean> httpEnabled;
    public static final ConfigValue<Boolean> httpWebsocketEnabled;
    public static final ConfigValue<List<? extends UnmodifiableConfig>> httpRules;

    public static final ConfigValue<Integer> httpMaxRequests;
    public static final ConfigValue<Integer> httpMaxWebsockets;

    public static final ConfigValue<Integer> httpDownloadBandwidth;
    public static final ConfigValue<Integer> httpUploadBandwidth;

    public static final ConfigValue<Boolean> commandBlockEnabled;
    public static final ConfigValue<Integer> modemRange;
    public static final ConfigValue<Integer> modemHighAltitudeRange;
    public static final ConfigValue<Integer> modemRangeDuringStorm;
    public static final ConfigValue<Integer> modemHighAltitudeRangeDuringStorm;
    public static final ConfigValue<Integer> maxNotesPerTick;
    public static final ConfigValue<Integer> monitorBandwidth;

    public static final ConfigValue<Boolean> turtlesNeedFuel;
    public static final ConfigValue<Integer> turtleFuelLimit;
    public static final ConfigValue<Integer> advancedTurtleFuelLimit;
    public static final ConfigValue<Boolean> turtlesCanPush;

    public static final ConfigValue<Integer> computerTermWidth;
    public static final ConfigValue<Integer> computerTermHeight;

    public static final ConfigValue<Integer> pocketTermWidth;
    public static final ConfigValue<Integer> pocketTermHeight;

    public static final ConfigValue<Integer> monitorWidth;
    public static final ConfigValue<Integer> monitorHeight;

    public static final ForgeConfigSpec clientSpec;

    public static final ConfigValue<MonitorRenderer> monitorRenderer;
    public static final ConfigValue<Integer> monitorDistance;
    public static final ConfigValue<Integer> uploadNagDelay;

    private static MarkerFilter logFilter = MarkerFilter.createFilter(Logging.COMPUTER_ERROR.getName(), Filter.Result.ACCEPT, Filter.Result.NEUTRAL);

    private ConfigSpec() {
    }

    static {
        LoggerContext.getContext().addFilter(logFilter);

        var builder = new TranslatingBuilder();

        { // General computers
            computerSpaceLimit = builder
                .comment("The disk space limit for computers and turtles, in bytes.")
                .define("computer_space_limit", Config.computerSpaceLimit);

            floppySpaceLimit = builder
                .comment("The disk space limit for floppy disks, in bytes.")
                .define("floppy_space_limit", Config.floppySpaceLimit);

            maximumFilesOpen = builder
                .comment("Set how many files a computer can have open at the same time. Set to 0 for unlimited.")
                .defineInRange("maximum_open_files", CoreConfig.maximumFilesOpen, 0, Integer.MAX_VALUE);

            disableLua51Features = builder
                .comment("""
                    Set this to true to disable Lua 5.1 functions that will be removed in a future
                    update. Useful for ensuring forward compatibility of your programs now.""")
                .define("disable_lua51_features", CoreConfig.disableLua51Features);

            defaultComputerSettings = builder
                .comment("""
                    A comma separated list of default system settings to set on new computers.
                    Example: "shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false"
                    will disable all autocompletion.""")
                .define("default_computer_settings", CoreConfig.defaultComputerSettings);

            logComputerErrors = builder
                .comment("""
                    Log exceptions thrown by peripherals and other Lua objects. This makes it easier
                    for mod authors to debug problems, but may result in log spam should people use
                    buggy methods.""")
                .define("log_computer_errors", true);

            commandRequireCreative = builder
                .comment("""
                    Require players to be in creative mode and be opped in order to interact with
                    command computers. This is the default behaviour for vanilla's Command blocks.""")
                .define("command_require_creative", Config.commandRequireCreative);
        }

        {
            builder.comment("""
                Controls execution behaviour of computers. This is largely intended for
                fine-tuning servers, and generally shouldn't need to be touched.""");
            builder.push("execution");

            computerThreads = builder
                .comment("""
                    Set the number of threads computers can run on. A higher number means more
                    computers can run at once, but may induce lag. Please note that some mods may
                    not work with a thread count higher than 1. Use with caution.""")
                .worldRestart()
                .defineInRange("computer_threads", 1, 1, Integer.MAX_VALUE);

            maxMainGlobalTime = builder
                .comment("""
                    The maximum time that can be spent executing tasks in a single tick, in
                    milliseconds.
                    Note, we will quite possibly go over this limit, as there's no way to tell how
                    long a will take - this aims to be the upper bound of the average time.""")
                .defineInRange("max_main_global_time", (int) TimeUnit.NANOSECONDS.toMillis(CoreConfig.maxMainGlobalTime), 1, Integer.MAX_VALUE);

            maxMainComputerTime = builder
                .comment("""
                    The ideal maximum time a computer can execute for in a tick, in milliseconds.
                    Note, we will quite possibly go over this limit, as there's no way to tell how
                    long a will take - this aims to be the upper bound of the average time.""")
                .defineInRange("max_main_computer_time", (int) TimeUnit.NANOSECONDS.toMillis(CoreConfig.maxMainComputerTime), 1, Integer.MAX_VALUE);

            builder.pop();
        }

        { // HTTP
            builder.comment("Controls the HTTP API");
            builder.push("http");

            httpEnabled = builder
                .comment("""
                    Enable the "http" API on Computers. This also disables the "pastebin" and "wget"
                    programs, that many users rely on. It's recommended to leave this on and use the
                    "rules" config option to impose more fine-grained control.""")
                .define("enabled", CoreConfig.httpEnabled);

            httpWebsocketEnabled = builder
                .comment("Enable use of http websockets. This requires the \"http_enable\" option to also be true.")
                .define("websocket_enabled", CoreConfig.httpWebsocketEnabled);

            httpRules = builder
                .comment("""
                    A list of rules which control behaviour of the "http" API for specific domains or
                    IPs. Each rule is an item with a 'host' to match against, and a series of
                    properties. Rules are evaluated in order, meaning earlier rules override later
                    ones.
                    The host may be a domain name ("pastebin.com"), wildcard ("*.pastebin.com") or
                    CIDR notation ("127.0.0.0/8").
                    If no rules, the domain is blocked.""")
                .defineList("rules", Arrays.asList(
                    AddressRuleConfig.makeRule("$private", Action.DENY),
                    AddressRuleConfig.makeRule("*", Action.ALLOW)
                ), x -> x instanceof UnmodifiableConfig && AddressRuleConfig.checkRule((UnmodifiableConfig) x));

            httpMaxRequests = builder
                .comment("""
                    The number of http requests a computer can make at one time. Additional requests
                    will be queued, and sent when the running requests have finished. Set to 0 for
                    unlimited.""")
                .defineInRange("max_requests", CoreConfig.httpMaxRequests, 0, Integer.MAX_VALUE);

            httpMaxWebsockets = builder
                .comment("The number of websockets a computer can have open at one time. Set to 0 for unlimited.")
                .defineInRange("max_websockets", CoreConfig.httpMaxWebsockets, 1, Integer.MAX_VALUE);

            builder
                .comment("Limits bandwidth used by computers.")
                .push("bandwidth");

            httpDownloadBandwidth = builder
                .comment("The number of bytes which can be downloaded in a second. This is shared across all computers. (bytes/s).")
                .defineInRange("global_download", CoreConfig.httpDownloadBandwidth, 1, Integer.MAX_VALUE);

            httpUploadBandwidth = builder
                .comment("The number of bytes which can be uploaded in a second. This is shared across all computers. (bytes/s).")
                .defineInRange("global_upload", CoreConfig.httpUploadBandwidth, 1, Integer.MAX_VALUE);

            builder.pop();

            builder.pop();
        }

        { // Peripherals
            builder.comment("Various options relating to peripherals.");
            builder.push("peripheral");

            commandBlockEnabled = builder
                .comment("Enable Command Block peripheral support")
                .define("command_block_enabled", Config.enableCommandBlock);

            modemRange = builder
                .comment("The range of Wireless Modems at low altitude in clear weather, in meters.")
                .defineInRange("modem_range", Config.modemRange, 0, MODEM_MAX_RANGE);

            modemHighAltitudeRange = builder
                .comment("The range of Wireless Modems at maximum altitude in clear weather, in meters.")
                .defineInRange("modem_high_altitude_range", Config.modemHighAltitudeRange, 0, MODEM_MAX_RANGE);

            modemRangeDuringStorm = builder
                .comment("The range of Wireless Modems at low altitude in stormy weather, in meters.")
                .defineInRange("modem_range_during_storm", Config.modemRangeDuringStorm, 0, MODEM_MAX_RANGE);

            modemHighAltitudeRangeDuringStorm = builder
                .comment("The range of Wireless Modems at maximum altitude in stormy weather, in meters.")
                .defineInRange("modem_high_altitude_range_during_storm", Config.modemHighAltitudeRangeDuringStorm, 0, MODEM_MAX_RANGE);

            maxNotesPerTick = builder
                .comment("Maximum amount of notes a speaker can play at once.")
                .defineInRange("max_notes_per_tick", Config.maxNotesPerTick, 1, Integer.MAX_VALUE);

            monitorBandwidth = builder
                .comment("""
                    The limit to how much monitor data can be sent *per tick*. Note:
                     - Bandwidth is measured before compression, so the data sent to the client is
                       smaller.
                     - This ignores the number of players a packet is sent to. Updating a monitor for
                       one player consumes the same bandwidth limit as sending to 20.
                     - A full sized monitor sends ~25kb of data. So the default (1MB) allows for ~40
                       monitors to be updated in a single tick.
                    Set to 0 to disable.""")
                .defineInRange("monitor_bandwidth", (int) Config.monitorBandwidth, 0, Integer.MAX_VALUE);

            builder.pop();
        }

        { // Turtles
            builder.comment("Various options relating to turtles.");
            builder.push("turtle");

            turtlesNeedFuel = builder
                .comment("Set whether Turtles require fuel to move.")
                .define("need_fuel", Config.turtlesNeedFuel);

            turtleFuelLimit = builder
                .comment("The fuel limit for Turtles.")
                .defineInRange("normal_fuel_limit", Config.turtleFuelLimit, 0, Integer.MAX_VALUE);

            advancedTurtleFuelLimit = builder
                .comment("The fuel limit for Advanced Turtles.")
                .defineInRange("advanced_fuel_limit", Config.advancedTurtleFuelLimit, 0, Integer.MAX_VALUE);

            turtlesCanPush = builder
                .comment("""
                    If set to true, Turtles will push entities out of the way instead of stopping if
                    there is space to do so.""")
                .define("can_push", Config.turtlesCanPush);

            builder.pop();
        }

        {
            builder
                .comment("""
                    Configure the size of various computer's terminals.
                    Larger terminals require more bandwidth, so use with care.""")
                .push("term_sizes");

            builder.comment("Terminal size of computers.").push("computer");
            computerTermWidth = builder.defineInRange("width", Config.computerTermWidth, 1, 255);
            computerTermHeight = builder.defineInRange("height", Config.computerTermHeight, 1, 255);
            builder.pop();

            builder.comment("Terminal size of pocket computers.").push("pocket_computer");
            pocketTermWidth = builder.defineInRange("width", Config.pocketTermWidth, 1, 255);
            pocketTermHeight = builder.defineInRange("height", Config.pocketTermHeight, 1, 255);
            builder.pop();

            builder.comment("Maximum size of monitors (in blocks).").push("monitor");
            monitorWidth = builder.defineInRange("width", Config.monitorWidth, 1, 32);
            monitorHeight = builder.defineInRange("height", Config.monitorHeight, 1, 32);
            builder.pop();

            builder.pop();
        }

        serverSpec = builder.build();

        var clientBuilder = new TranslatingBuilder();
        monitorRenderer = clientBuilder
            .comment("""
                The renderer to use for monitors. Generally this should be kept at "best" - if
                monitors have performance issues, you may wish to experiment with alternative
                renderers.""")
            .defineEnum("monitor_renderer", MonitorRenderer.BEST);
        monitorDistance = clientBuilder
            .comment("""
                The maximum distance monitors will render at. This defaults to the standard tile
                entity limit, but may be extended if you wish to build larger monitors.""")
            .defineInRange("monitor_distance", 64, 16, 1024);
        uploadNagDelay = clientBuilder
            .comment("The delay in seconds after which we'll notify about unhandled imports. Set to 0 to disable.")
            .defineInRange("upload_nag_delay", Config.uploadNagDelay, 0, 60);

        clientSpec = clientBuilder.build();
    }

    private static void syncServer() {
        if (!serverSpec.isLoaded()) return;

        // General
        Config.computerSpaceLimit = computerSpaceLimit.get();
        Config.floppySpaceLimit = floppySpaceLimit.get();
        CoreConfig.maximumFilesOpen = maximumFilesOpen.get();
        CoreConfig.disableLua51Features = disableLua51Features.get();
        CoreConfig.defaultComputerSettings = defaultComputerSettings.get();
        Config.commandRequireCreative = commandRequireCreative.get();

        // Execution
        CoreConfig.maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos(maxMainGlobalTime.get());
        CoreConfig.maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos(maxMainComputerTime.get());

        // Update our log filter if needed.
        var logFilter = MarkerFilter.createFilter(
            Logging.COMPUTER_ERROR.getName(),
            logComputerErrors.get() ? Filter.Result.ACCEPT : Filter.Result.DENY,
            Filter.Result.NEUTRAL
        );
        if (!logFilter.equals(ConfigSpec.logFilter)) {
            LoggerContext.getContext().removeFilter(ConfigSpec.logFilter);
            LoggerContext.getContext().addFilter(ConfigSpec.logFilter = logFilter);
        }

        // HTTP
        CoreConfig.httpEnabled = httpEnabled.get();
        CoreConfig.httpWebsocketEnabled = httpWebsocketEnabled.get();
        CoreConfig.httpRules = httpRules.get().stream()
            .map(AddressRuleConfig::parseRule).filter(Objects::nonNull).toList();

        CoreConfig.httpMaxRequests = httpMaxRequests.get();
        CoreConfig.httpMaxWebsockets = httpMaxWebsockets.get();
        CoreConfig.httpDownloadBandwidth = httpDownloadBandwidth.get();
        CoreConfig.httpUploadBandwidth = httpUploadBandwidth.get();
        NetworkUtils.reloadConfig();

        // Peripheral
        Config.enableCommandBlock = commandBlockEnabled.get();
        Config.maxNotesPerTick = maxNotesPerTick.get();
        Config.modemRange = modemRange.get();
        Config.modemHighAltitudeRange = modemHighAltitudeRange.get();
        Config.modemRangeDuringStorm = modemRangeDuringStorm.get();
        Config.modemHighAltitudeRangeDuringStorm = modemHighAltitudeRangeDuringStorm.get();
        Config.monitorBandwidth = monitorBandwidth.get();

        // Turtles
        Config.turtlesNeedFuel = turtlesNeedFuel.get();
        Config.turtleFuelLimit = turtleFuelLimit.get();
        Config.advancedTurtleFuelLimit = advancedTurtleFuelLimit.get();
        Config.turtlesCanPush = turtlesCanPush.get();

        // Terminal size
        Config.computerTermWidth = computerTermWidth.get();
        Config.computerTermHeight = computerTermHeight.get();
        Config.pocketTermWidth = pocketTermWidth.get();
        Config.pocketTermHeight = pocketTermHeight.get();
        Config.monitorWidth = monitorWidth.get();
        Config.monitorHeight = monitorHeight.get();
    }

    private static void syncClient() {
        if (!clientSpec.isLoaded()) return;

        Config.monitorRenderer = monitorRenderer.get();
        Config.monitorDistance = monitorDistance.get();
        Config.uploadNagDelay = uploadNagDelay.get();
    }

    public static void sync(ModConfig config) {
        if (!config.getModId().equals(ComputerCraftAPI.MOD_ID)) return;

        if (config.getType() == ModConfig.Type.SERVER) syncServer();
        if (config.getType() == ModConfig.Type.CLIENT) syncClient();
    }

    /**
     * A {@link ForgeConfigSpec.Builder} which adds translation keys to every entry.
     */
    private static class TranslatingBuilder {
        private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        private final Deque<String> groupStack = new ArrayDeque<>();

        private void translation(String name) {
            var key = new StringBuilder(TRANSLATION_PREFIX);
            for (var group : groupStack) key.append(group).append('.');
            key.append(name);
            builder.translation(key.toString());
        }

        public TranslatingBuilder comment(String comment) {
            builder.comment(comment);
            return this;
        }

        public TranslatingBuilder push(String name) {
            translation(name);
            builder.push(name);
            groupStack.addLast(name);
            return this;
        }

        public TranslatingBuilder pop() {
            builder.pop();
            groupStack.removeLast();
            return this;
        }

        public ForgeConfigSpec build() {
            return builder.build();
        }

        public TranslatingBuilder worldRestart() {
            builder.worldRestart();
            return this;
        }

        public <T> ConfigValue<T> define(String path, T defaultValue) {
            translation(path);
            return builder.define(path, defaultValue);
        }

        public ConfigValue<Boolean> define(String path, boolean defaultValue) {
            translation(path);
            return builder.define(path, defaultValue);
        }

        public ConfigValue<Integer> defineInRange(String path, int defaultValue, int min, int max) {
            translation(path);
            return builder.defineInRange(path, defaultValue, min, max);
        }

        public <T> ConfigValue<List<? extends T>> defineList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator) {
            translation(path);
            return builder.defineList(path, defaultValue, elementValidator);
        }

        public <V extends Enum<V>> ConfigValue<V> defineEnum(String path, V defaultValue) {
            translation(path);
            return builder.defineEnum(path, defaultValue);
        }
    }
}
