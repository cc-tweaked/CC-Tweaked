/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRuleConfig;
import dan200.computercraft.fabric.mixin.LevelResourceAccess;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class Config
{
    private static final int MODEM_MAX_RANGE = 100000;

    public static final String TRANSLATION_PREFIX = "gui.computercraft.config.";

    public static final CommentedConfigSpec serverSpec;
    public static final CommentedConfigSpec clientSpec;

    public static CommentedFileConfig serverConfig;
    public static CommentedFileConfig clientConfig;

    private static final LevelResource serverDir = LevelResourceAccess.create( "serverconfig" );
    private static final String serverFileName = "computercraft-server.toml";

    private static Path serverPath = null;
    private static final Path clientPath = FabricLoader.getInstance().getConfigDir().resolve( "computercraft-client.toml" );

    private Config()
    {
    }

    static
    {
        System.setProperty( "nightconfig.preserveInsertionOrder", "true" );

        serverSpec = new CommentedConfigSpec();
        { // General computers
            serverSpec.comment( "computer_space_limit",
                "The disk space limit for computers and turtles, in bytes" );
            serverSpec.define( "computer_space_limit", ComputerCraft.computerSpaceLimit );

            serverSpec.comment( "floppy_space_limit",
                "The disk space limit for floppy disks, in bytes" );
            serverSpec.define( "floppy_space_limit", ComputerCraft.floppySpaceLimit );

            serverSpec.comment( "maximum_open_files",
                "Set how many files a computer can have open at the same time. Set to 0 for unlimited." );
            serverSpec.defineInRange( "maximum_open_files", ComputerCraft.maximumFilesOpen, 0, Integer.MAX_VALUE );

            serverSpec.comment( "disable_lua51_features",
                "Set this to true to disable Lua 5.1 functions that will be removed in a future update. " +
                    "Useful for ensuring forward compatibility of your programs now." );
            serverSpec.define( "disable_lua51_features", ComputerCraft.disableLua51Features );

            serverSpec.comment( "default_computer_settings",
                "A comma separated list of default system settings to set on new computers. Example: " +
                    "\"shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false\" will disable all " +
                    "autocompletion" );
            serverSpec.define( "default_computer_settings", ComputerCraft.defaultComputerSettings );

            serverSpec.comment( "log_computer_errors",
                "Log exceptions thrown by peripherals and other Lua objects.\n" +
                    "This makes it easier for mod authors to debug problems, but may result in log spam should people use buggy methods." );
            serverSpec.define( "log_computer_errors", ComputerCraft.logComputerErrors );

            serverSpec.comment( "command_require_creative",
                "Require players to be in creative mode and be opped in order to interact with command computers." +
                    "This is the default behaviour for vanilla's Command blocks." );
            serverSpec.define( "command_require_creative", ComputerCraft.commandRequireCreative );
        }

        { // Execution
            serverSpec.comment( "execution",
                "Controls execution behaviour of computers. This is largely intended for fine-tuning " +
                    "servers, and generally shouldn't need to be touched" );

            serverSpec.comment( "execution.computer_threads",
                "Set the number of threads computers can run on. A higher number means more computers can run " +
                    "at once, but may induce lag.\n" +
                    "Please note that some mods may not work with a thread count higher than 1. Use with caution." );
            serverSpec.defineInRange( "execution.computer_threads", ComputerCraft.computerThreads, 1, Integer.MAX_VALUE );

            serverSpec.comment( "execution.max_main_global_time",
                "The maximum time that can be spent executing tasks in a single tick, in milliseconds.\n" +
                    "Note, we will quite possibly go over this limit, as there's no way to tell how long a will take " +
                    "- this aims to be the upper bound of the average time." );
            serverSpec.defineInRange( "execution.max_main_global_time", (int) TimeUnit.NANOSECONDS.toMillis( ComputerCraft.maxMainGlobalTime ), 1, Integer.MAX_VALUE );

            serverSpec.comment( "execution.max_main_computer_time",
                "The ideal maximum time a computer can execute for in a tick, in milliseconds.\n" +
                    "Note, we will quite possibly go over this limit, as there's no way to tell how long a will take " +
                    "- this aims to be the upper bound of the average time." );
            serverSpec.defineInRange( "execution.max_main_computer_time", (int) TimeUnit.NANOSECONDS.toMillis( ComputerCraft.maxMainComputerTime ), 1, Integer.MAX_VALUE );
        }

        { // HTTP
            serverSpec.comment( "http", "Controls the HTTP API" );

            serverSpec.comment( "http.enabled",
                "Enable the \"http\" API on Computers (see \"rules\" for more fine grained control than this)." );
            serverSpec.define( "http.enabled", ComputerCraft.httpEnabled );

            serverSpec.comment( "http.websocket_enabled",
                "Enable use of http websockets. This requires the \"http_enable\" option to also be true." );
            serverSpec.define( "http.websocket_enabled", ComputerCraft.httpWebsocketEnabled );

            serverSpec.comment( "http.rules",
                "A list of rules which control behaviour of the \"http\" API for specific domains or IPs.\n" +
                    "Each rule is an item with a 'host' to match against, and a series of properties. " +
                    "The host may be a domain name (\"pastebin.com\"),\n" +
                    "wildcard (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\"). If no rules, the domain is blocked." );
            serverSpec.defineList( "http.rules", Arrays.asList(
                AddressRuleConfig.makeRule( "$private", Action.DENY ),
                AddressRuleConfig.makeRule( "*", Action.ALLOW )
            ), x -> x instanceof UnmodifiableConfig && AddressRuleConfig.checkRule( (UnmodifiableConfig) x ) );

            serverSpec.comment( "http.max_requests",
                "The number of http requests a computer can make at one time. Additional requests will be queued, and sent when the running requests have finished. Set to 0 for unlimited." );
            serverSpec.defineInRange( "http.max_requests", ComputerCraft.httpMaxRequests, 0, Integer.MAX_VALUE );

            serverSpec.comment( "http.max_websockets",
                "The number of websockets a computer can have open at one time. Set to 0 for unlimited." );
            serverSpec.defineInRange( "http.max_websockets", ComputerCraft.httpMaxWebsockets, 1, Integer.MAX_VALUE );
        }

        { // Peripherals
            serverSpec.comment( "peripheral", "Various options relating to peripherals." );

            serverSpec.comment( "peripheral.command_block_enabled",
                "Enable Command Block peripheral support" );
            serverSpec.define( "peripheral.command_block_enabled", ComputerCraft.enableCommandBlock );

            serverSpec.comment( "peripheral.modem_range",
                "The range of Wireless Modems at low altitude in clear weather, in meters" );
            serverSpec.defineInRange( "peripheral.modem_range", ComputerCraft.modemRange, 0, MODEM_MAX_RANGE );

            serverSpec.comment( "peripheral.modem_high_altitude_range",
                "The range of Wireless Modems at maximum altitude in clear weather, in meters" );
            serverSpec.defineInRange( "peripheral.modem_high_altitude_range", ComputerCraft.modemHighAltitudeRange, 0, MODEM_MAX_RANGE );

            serverSpec.comment( "peripheral.modem_range_during_storm",
                "The range of Wireless Modems at low altitude in stormy weather, in meters" );
            serverSpec.defineInRange( "peripheral.modem_range_during_storm", ComputerCraft.modemRangeDuringStorm, 0, MODEM_MAX_RANGE );

            serverSpec.comment( "peripheral.modem_high_altitude_range_during_storm",
                "The range of Wireless Modems at maximum altitude in stormy weather, in meters" );
            serverSpec.defineInRange( "peripheral.modem_high_altitude_range_during_storm", ComputerCraft.modemHighAltitudeRangeDuringStorm, 0, MODEM_MAX_RANGE );

            serverSpec.comment( "peripheral.max_notes_per_tick",
                "Maximum amount of notes a speaker can play at once" );
            serverSpec.defineInRange( "peripheral.max_notes_per_tick", ComputerCraft.maxNotesPerTick, 1, Integer.MAX_VALUE );

            serverSpec.comment( "peripheral.monitor_bandwidth",
                "The limit to how much monitor data can be sent *per tick*. Note:\n" +
                    " - Bandwidth is measured before compression, so the data sent to the client is smaller.\n" +
                    " - This ignores the number of players a packet is sent to. Updating a monitor for one player consumes " +
                    "the same bandwidth limit as sending to 20.\n" +
                    " - A full sized monitor sends ~25kb of data. So the default (1MB) allows for ~40 monitors to be updated " +
                    "in a single tick. \n" +
                    "Set to 0 to disable." );
            serverSpec.defineInRange( "peripheral.monitor_bandwidth", (int) ComputerCraft.monitorBandwidth, 0, Integer.MAX_VALUE );
        }

        { // Turtles
            serverSpec.comment( "turtle", "Various options relating to turtles." );

            serverSpec.comment( "turtle.need_fuel",
                "Set whether Turtles require fuel to move" );
            serverSpec.define( "turtle.need_fuel", ComputerCraft.turtlesNeedFuel );

            serverSpec.comment( "turtle.normal_fuel_limit", "The fuel limit for Turtles" );
            serverSpec.defineInRange( "turtle.normal_fuel_limit", ComputerCraft.turtleFuelLimit, 0, Integer.MAX_VALUE );

            serverSpec.comment( "turtle.advanced_fuel_limit",
                "The fuel limit for Advanced Turtles" );
            serverSpec.defineInRange( "turtle.advanced_fuel_limit", ComputerCraft.advancedTurtleFuelLimit, 0, Integer.MAX_VALUE );

            serverSpec.comment( "turtle.obey_block_protection",
                "If set to true, Turtles will be unable to build, dig, or enter protected areas (such as near the server spawn point)" );
            serverSpec.define( "turtle.obey_block_protection", ComputerCraft.turtlesObeyBlockProtection );

            serverSpec.comment( "turtle.can_push",
                "If set to true, Turtles will push entities out of the way instead of stopping if there is space to do so" );
            serverSpec.define( "turtle.can_push", ComputerCraft.turtlesCanPush );
        }

        { // Terminal sizes
            serverSpec.comment( "term_sizes", "Configure the size of various computer's terminals.\n" +
                "Larger terminals require more bandwidth, so use with care." );

            serverSpec.comment( "term_sizes.computer", "Terminal size of computers" );
            serverSpec.defineInRange( "term_sizes.computer.width", ComputerCraft.computerTermWidth, 1, 255 );
            serverSpec.defineInRange( "term_sizes.computer.height", ComputerCraft.computerTermHeight, 1, 255 );

            serverSpec.comment( "term_sizes.pocket_computer", "Terminal size of pocket computers" );
            serverSpec.defineInRange( "term_sizes.pocket_computer.width", ComputerCraft.pocketTermWidth, 1, 255 );
            serverSpec.defineInRange( "term_sizes.pocket_computer.height", ComputerCraft.pocketTermHeight, 1, 255 );

            serverSpec.comment( "term_sizes.monitor", "Maximum size of monitors (in blocks)" );
            serverSpec.defineInRange( "term_sizes.monitor.width", ComputerCraft.monitorWidth, 1, 32 );
            serverSpec.defineInRange( "term_sizes.monitor.height", ComputerCraft.monitorHeight, 1, 32 );
        }

        clientSpec = new CommentedConfigSpec();

        clientSpec.comment( "monitor_renderer",
            "The renderer to use for monitors. Generally this should be kept at \"best\" - if " +
                "monitors have performance issues, you may wish to experiment with alternative renderers." );
        clientSpec.defineRestrictedEnum( "monitor_renderer", MonitorRenderer.BEST, EnumSet.allOf( MonitorRenderer.class ), EnumGetMethod.NAME_IGNORECASE );

        clientSpec.comment( "monitor_distance",
            "The maximum distance monitors will render at. This defaults to the standard tile entity limit, " +
                "but may be extended if you wish to build larger monitors." );
        clientSpec.defineInRange( "monitor_distance", 64, 16, 1024 );
    }

    private static final FileNotFoundAction MAKE_DIRECTORIES_AND_FILE = ( file, configFormat ) -> {
        Files.createDirectories( file.getParent() );
        Files.createFile( file );
        configFormat.initEmptyFile( file );
        return false;
    };

    private static CommentedFileConfig buildFileConfig( Path path )
    {
        return CommentedFileConfig.builder( path )
            .onFileNotFound( MAKE_DIRECTORIES_AND_FILE )
            .preserveInsertionOrder()
            .build();
    }

    private static void saveConfig( UnmodifiableConfig config, CommentedConfigSpec spec, Path path )
    {
        try( CommentedFileConfig fileConfig = buildFileConfig( path ) )
        {
            fileConfig.putAll( config );
            spec.correct( fileConfig );
            fileConfig.save();
        }
    }

    public static void save()
    {
        if( clientConfig != null )
        {
            saveConfig( clientConfig, clientSpec, clientPath );
        }
        if( serverConfig != null && serverPath != null )
        {
            saveConfig( serverConfig, serverSpec, serverPath );
        }
    }

    public static void serverStarting( MinecraftServer server )
    {
        serverPath = server.getWorldPath( serverDir ).resolve( serverFileName );

        try( CommentedFileConfig config = buildFileConfig( serverPath ) )
        {
            config.load();
            serverSpec.correct( config, Config::correctionListener );
            config.save();
            serverConfig = config;
            sync();
        }
    }

    public static void serverStopping( MinecraftServer server )
    {
        serverConfig = null;
        serverPath = null;
    }

    public static void clientStarted( Minecraft client )
    {
        try( CommentedFileConfig config = buildFileConfig( clientPath ) )
        {
            config.load();
            clientSpec.correct( config, Config::correctionListener );
            config.save();
            clientConfig = config;
            sync();
        }
    }

    private static void correctionListener( ConfigSpec.CorrectionAction action, List<String> path, Object incorrectValue, Object correctedValue )
    {
        String key = String.join( ".", path );
        switch( action )
        {
            case ADD:
                ComputerCraft.log.warn( "Config key {} missing -> added default value.", key );
                break;
            case REMOVE:
                ComputerCraft.log.warn( "Config key {} not defined -> removed from config.", key );
                break;
            case REPLACE:
                ComputerCraft.log.warn( "Config key {} not valid -> replaced with default value.", key );
        }
    }

    public static void sync()
    {
        if( serverConfig != null )
        {
            // General
            ComputerCraft.computerSpaceLimit = serverConfig.<Integer>get( "computer_space_limit" );
            ComputerCraft.floppySpaceLimit = serverConfig.<Integer>get( "floppy_space_limit" );
            ComputerCraft.maximumFilesOpen = serverConfig.<Integer>get( "maximum_open_files" );
            ComputerCraft.disableLua51Features = serverConfig.<Boolean>get( "disable_lua51_features" );
            ComputerCraft.defaultComputerSettings = serverConfig.<String>get( "default_computer_settings" );
            ComputerCraft.logComputerErrors = serverConfig.<Boolean>get( "log_computer_errors" );
            ComputerCraft.commandRequireCreative = serverConfig.<Boolean>get( "command_require_creative" );

            // Execution
            ComputerCraft.computerThreads = serverConfig.<Integer>get( "execution.computer_threads" );
            ComputerCraft.maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos( serverConfig.<Integer>get( "execution.max_main_global_time" ) );
            ComputerCraft.maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos( serverConfig.<Integer>get( "execution.max_main_computer_time" ) );

            // HTTP
            ComputerCraft.httpEnabled = serverConfig.<Boolean>get( "http.enabled" );
            ComputerCraft.httpWebsocketEnabled = serverConfig.<Boolean>get( "http.websocket_enabled" );
            ComputerCraft.httpRules = serverConfig.<List<UnmodifiableConfig>>get( "http.rules" ).stream().map( AddressRuleConfig::parseRule )
                .filter( Objects::nonNull ).collect( Collectors.toList() );
            ComputerCraft.httpMaxRequests = serverConfig.<Integer>get( "http.max_requests" );
            ComputerCraft.httpMaxWebsockets = serverConfig.<Integer>get( "http.max_websockets" );

            // Peripherals
            ComputerCraft.enableCommandBlock = serverConfig.<Boolean>get( "peripheral.command_block_enabled" );
            ComputerCraft.modemRange = serverConfig.<Integer>get( "peripheral.modem_range" );
            ComputerCraft.modemHighAltitudeRange = serverConfig.<Integer>get( "peripheral.modem_high_altitude_range" );
            ComputerCraft.modemRangeDuringStorm = serverConfig.<Integer>get( "peripheral.modem_range_during_storm" );
            ComputerCraft.modemHighAltitudeRangeDuringStorm = serverConfig.<Integer>get( "peripheral.modem_high_altitude_range_during_storm" );
            ComputerCraft.maxNotesPerTick = serverConfig.<Integer>get( "peripheral.max_notes_per_tick" );
            ComputerCraft.monitorBandwidth = serverConfig.<Integer>get( "peripheral.monitor_bandwidth" );

            // Turtles
            ComputerCraft.turtlesNeedFuel = serverConfig.<Boolean>get( "turtle.need_fuel" );
            ComputerCraft.turtleFuelLimit = serverConfig.<Integer>get( "turtle.normal_fuel_limit" );
            ComputerCraft.advancedTurtleFuelLimit = serverConfig.<Integer>get( "turtle.advanced_fuel_limit" );
            ComputerCraft.turtlesObeyBlockProtection = serverConfig.<Boolean>get( "turtle.obey_block_protection" );
            ComputerCraft.turtlesCanPush = serverConfig.<Boolean>get( "turtle.can_push" );

            // Terminal Size
            ComputerCraft.computerTermWidth = serverConfig.<Integer>get( "term_sizes.computer.width" );
            ComputerCraft.computerTermHeight = serverConfig.<Integer>get( "term_sizes.computer.height" );
            ComputerCraft.pocketTermWidth = serverConfig.<Integer>get( "term_sizes.pocket_computer.width" );
            ComputerCraft.pocketTermHeight = serverConfig.<Integer>get( "term_sizes.pocket_computer.height" );
            ComputerCraft.monitorWidth = serverConfig.<Integer>get( "term_sizes.monitor.width" );
            ComputerCraft.monitorHeight = serverConfig.<Integer>get( "term_sizes.monitor.height" );
        }

        // Client
        if( clientConfig != null )
        {
            ComputerCraft.monitorRenderer = clientConfig.getEnum( "monitor_renderer", MonitorRenderer.class );
            ComputerCraft.monitorDistance = clientConfig.get( "monitor_distance" );
        }
    }

    private static final Converter<String, String> converter = CaseFormat.LOWER_CAMEL.converterTo( CaseFormat.UPPER_UNDERSCORE );
}
