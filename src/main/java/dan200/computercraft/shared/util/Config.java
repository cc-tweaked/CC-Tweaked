package dan200.computercraft.shared.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.core.apis.AddressPredicate;
import dan200.computercraft.core.apis.http.websocket.Websocket;

public class Config {
    public static final transient int MODEM_MAX_RANGE = 100000;
    public static final transient Config defaultConfig = new Config();
    private static transient Path configPath;
    private static transient Config config;
    public General general = new General();
    @Comment ("\nControls execution behaviour of computers. This is largely intended for fine-tuning " + "servers, and generally shouldn't need to be " +
              "touched") public Execution execution = new Execution();
    @Comment ("\nControls the HTTP API") public Http http = new Http();
    @Comment ("\nVarious options relating to peripherals.") public Peripheral peripheral = new Peripheral();
    @Comment ("\nVarious options relating to turtles.") public Turtle turtle = new Turtle();

    public static Config get() {
        return config;
    }

    public static void load(Path path) {
        configPath = path;

        if (Files.exists(configPath)) {
            Jankson jankson = Jankson.builder()
                                     .build();
            try {
                JsonObject jsonObject = jankson.load(Files.newInputStream(configPath));
                config = jankson.fromJson(jsonObject, Config.class);
            } catch (IOException | SyntaxError e) {
                config = new Config();
                ComputerCraft.log.error("Failed to load config! Use default config.");
                e.printStackTrace();
                return;
            }
        } else {
            config = new Config();
        }
        save();
        sync();
    }

    public static void save() {
        Jankson jankson = Jankson.builder()
                                 .build();
        try {
            String configData = jankson.toJson(config)
                                       .toJson(true, true);
            Files.write(configPath, configData.getBytes());
        } catch (IOException e) {
            ComputerCraft.log.error("Failed to save config!");
            e.printStackTrace();
        }
    }

    public static void sync() {
        // General
        ComputerCraft.computerSpaceLimit = config.general.computer_space_limit;
        ComputerCraft.floppySpaceLimit = config.general.floppy_space_limit;
        ComputerCraft.maximumFilesOpen = Math.max(0, config.general.maximum_open_files);
        ComputerCraft.disable_lua51_features = config.general.disable_lua51_features;
        ComputerCraft.default_computer_settings = config.general.default_computer_settings;
        ComputerCraft.debug_enable = config.general.debug_enabled;
        ComputerCraft.logPeripheralErrors = config.general.log_computer_errors;

        // Execution
        ComputerCraft.computer_threads = Math.max(1, config.execution.computer_threads);
        ComputerCraft.maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos(Math.max(1, config.execution.max_main_global_time));
        ComputerCraft.maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos(Math.max(1, config.execution.max_main_computer_time));

        // HTTP
        ComputerCraft.http_enable = config.http.enabled;
        ComputerCraft.http_websocket_enable = config.http.websocket_enabled;
        ComputerCraft.http_whitelist = new AddressPredicate(config.http.whitelist);
        ComputerCraft.http_blacklist = new AddressPredicate(config.http.blacklist);

        ComputerCraft.httpTimeout = Math.max(0, config.http.timeout);
        ComputerCraft.httpMaxRequests = Math.max(1, config.http.max_requests);
        ComputerCraft.httpMaxDownload = Math.max(0, config.http.max_download);
        ComputerCraft.httpMaxUpload = Math.max(0, config.http.max_upload);
        ComputerCraft.httpMaxWebsockets = Math.max(1, config.http.max_websockets);
        ComputerCraft.httpMaxWebsocketMessage = Math.min(Math.max(0, config.http.max_websocket_message), Websocket.MAX_MESSAGE_SIZE);

        // Peripheral
        ComputerCraft.enableCommandBlock = config.peripheral.command_block_enabled;
        ComputerCraft.maxNotesPerTick = Math.max(1, config.peripheral.max_notes_per_tick);
        ComputerCraft.modem_range = Math.min(Math.max(0, config.peripheral.modem_range), MODEM_MAX_RANGE);
        ComputerCraft.modem_highAltitudeRange = Math.min(Math.max(0, config.peripheral.modem_high_altitude_range), MODEM_MAX_RANGE);
        ComputerCraft.modem_rangeDuringStorm = Math.min(Math.max(0, config.peripheral.modem_range_during_storm), MODEM_MAX_RANGE);
        ComputerCraft.modem_highAltitudeRangeDuringStorm = Math.min(Math.max(0, config.peripheral.modem_high_altitude_range_during_storm), MODEM_MAX_RANGE);

        // Turtles
        ComputerCraft.turtlesNeedFuel = config.turtle.need_fuel;
        ComputerCraft.turtleFuelLimit = Math.max(0, config.turtle.normal_fuel_limit);
        ComputerCraft.advancedTurtleFuelLimit = Math.max(0, config.turtle.advanced_fuel_limit);
        ComputerCraft.turtlesObeyBlockProtection = config.turtle.obey_block_protection;
        ComputerCraft.turtlesCanPush = config.turtle.can_push;

        ComputerCraft.turtleDisabledActions.clear();
        Converter<String, String> converter = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE);
        for (String value : config.turtle.disabled_actions) {
            try {
                ComputerCraft.turtleDisabledActions.add(TurtleAction.valueOf(converter.convert(value)));
            } catch (IllegalArgumentException e) {
                ComputerCraft.log.error("Unknown turtle action " + value);
            }
        }
    }

    public static class General {
        @Comment ("\nThe disk space limit for computers and turtles, in bytes") public int computer_space_limit = ComputerCraft.computerSpaceLimit;

        @Comment ("\nThe disk space limit for floppy disks, in bytes") public int floppy_space_limit = ComputerCraft.floppySpaceLimit;

        @Comment ("\nSet how many files a computer can have open at the same time. Set to 0 for unlimited.") public int maximum_open_files =
            ComputerCraft.maximumFilesOpen;

        @Comment ("\nSet this to true to disable Lua 5.1 functions that will be removed in a future " + "update. Useful for ensuring forward " +
                  "compatibility of your programs now.") public boolean disable_lua51_features = ComputerCraft.disable_lua51_features;

        @Comment ("\nA comma separated list of default system settings to set on new computers. Example: " + "\"shell.autocomplete=false,lua" +
                  ".autocomplete=false,edit.autocomplete=false\" will disable all autocompletion") public String default_computer_settings =
            ComputerCraft.default_computer_settings;

        @Comment ("\nEnable Lua's debug library. This is sandboxed to each computer, so is generally safe to be used by players.") public boolean debug_enabled = ComputerCraft.debug_enable;

        @Comment ("\nLog exceptions thrown by peripherals and other Lua objects.\n" + "This makes it easier for mod authors to debug problems, but may " +
                  "result in log spam should people use buggy methods.") public boolean log_computer_errors = ComputerCraft.logPeripheralErrors;
    }

    public static class Execution {
        @Comment ("\nSet the number of threads computers can run on. A higher number means more computers can " + "run at once, but may induce lag.\n" +
                  "Please note that some mods may not work with a thread count higher than 1. Use with caution.") public int computer_threads =
            ComputerCraft.computer_threads;

        @Comment ("\nThe maximum time that can be spent executing tasks in a single tick, in milliseconds.\n" + "Note, we will quite possibly go over " +
                  "this limit, as there's no way to tell how long a will take - this aims " + "to be the upper bound of the average time.") public long max_main_global_time = TimeUnit.NANOSECONDS.toMillis(
            ComputerCraft.maxMainGlobalTime);

        @Comment ("\nThe ideal maximum time a computer can execute for in a tick, in milliseconds.\n" + "Note, we will quite possibly go over this limit," +
                  " as there's no way to tell how long a will take - this aims " + "to be the upper bound of the average time.") public long max_main_computer_time = TimeUnit.NANOSECONDS.toMillis(
            ComputerCraft.maxMainComputerTime);
    }

    public static class Http {
        @Comment ("\nEnable the \"http\" API on Computers (see \"http_whitelist\" and \"http_blacklist\" for " + "more fine grained control than this)") public boolean enabled = ComputerCraft.http_enable;

        @Comment ("\nEnable use of http websockets. This requires the \"http_enable\" option to also be true.") public boolean websocket_enabled =
            ComputerCraft.http_websocket_enable;

        @Comment ("\nA list of wildcards for domains or IP ranges that can be accessed through the " + "\"http\" API on Computers.\n" + "Set this to " +
                  "\"*\" to access to the entire internet. Example: \"*.pastebin.com\" will restrict access to " + "just subdomains of pastebin.com.\n" + "You can use domain names (\"pastebin.com\"), wilcards (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\").") public String[] whitelist = ComputerCraft.DEFAULT_HTTP_WHITELIST.clone();

        @Comment ("\nA list of wildcards for domains or IP ranges that cannot be accessed through the " + "\"http\" API on Computers.\n" + "If this is " +
                  "empty then all whitelisted domains will be accessible. Example: \"*.github.com\" will block " + "access to all subdomains of github" +
                  ".com.\n" + "You can use domain names (\"pastebin.com\"), wilcards (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\").") public String[] blacklist = ComputerCraft.DEFAULT_HTTP_BLACKLIST.clone();

        @Comment ("\nThe period of time (in milliseconds) to wait before a HTTP request times out. Set to 0 for unlimited.") public int timeout =
            ComputerCraft.httpTimeout;

        @Comment ("\nThe number of http requests a computer can make at one time. Additional requests " + "will be queued, and sent when the running " +
                  "requests have finished. Set to 0 for unlimited.") public int max_requests = ComputerCraft.httpMaxRequests;

        @Comment ("\nThe maximum size (in bytes) that a computer can download in a single request. " + "Note that responses may receive more data than " +
                  "allowed, but this data will not be returned to the client.") public long max_download = ComputerCraft.httpMaxDownload;

        @Comment ("\nThe maximum size (in bytes) that a computer can upload in a single request. This " + "includes headers and POST text.") public long max_upload = ComputerCraft.httpMaxUpload;

        @Comment ("\nThe number of websockets a computer can have open at one time. Set to 0 for unlimited.") public int max_websockets =
            ComputerCraft.httpMaxWebsockets;

        @Comment ("\nThe maximum size (in bytes) that a computer can send or receive in one websocket packet.") public int max_websocket_message =
            ComputerCraft.httpMaxWebsocketMessage;
    }

    public static class Peripheral {
        @Comment ("\n\nEnable Command Block peripheral support") public boolean command_block_enabled = ComputerCraft.enableCommandBlock;

        @Comment ("\nThe range of Wireless Modems at low altitude in clear weather, in meters") public int modem_range = ComputerCraft.modem_range;

        @Comment ("\nThe range of Wireless Modems at maximum altitude in clear weather, in meters") public int modem_high_altitude_range =
            ComputerCraft.modem_highAltitudeRange;

        @Comment ("\nThe range of Wireless Modems at low altitude in stormy weather, in meters") public int modem_range_during_storm =
            ComputerCraft.modem_rangeDuringStorm;

        @Comment ("\nThe range of Wireless Modems at maximum altitude in stormy weather, in meters") public int modem_high_altitude_range_during_storm =
            ComputerCraft.modem_highAltitudeRangeDuringStorm;

        @Comment ("\nMaximum amount of notes a speaker can play at once") public int max_notes_per_tick = ComputerCraft.maxNotesPerTick;
    }

    public static class Turtle {
        @Comment ("\nSet whether Turtles require fuel to move") public boolean need_fuel = ComputerCraft.turtlesNeedFuel;

        @Comment ("\nThe fuel limit for Turtles") public int normal_fuel_limit = ComputerCraft.turtleFuelLimit;

        @Comment ("\nThe fuel limit for Advanced Turtles") public int advanced_fuel_limit = ComputerCraft.advancedTurtleFuelLimit;

        @Comment ("\nIf set to true, Turtles will be unable to build, dig, or enter protected " + "areas (such as near the server spawn point)") public boolean obey_block_protection = ComputerCraft.turtlesObeyBlockProtection;

        @Comment ("\nIf set to true, Turtles will push entities out of the way instead of stopping if " + "there is space to do so") public boolean can_push = ComputerCraft.turtlesCanPush;

        @Comment ("\nA list of turtle actions which are disabled.") public String[] disabled_actions = new String[0];
    }
}
