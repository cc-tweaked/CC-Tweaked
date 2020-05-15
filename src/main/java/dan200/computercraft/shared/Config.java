/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRuleConfig;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraftforge.common.ForgeConfigSpec.Builder;
import static net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class Config
{
    private static final int MODEM_MAX_RANGE = 100000;

    private static final String TRANSLATION_PREFIX = "gui.computercraft.config.";

    private static final ConfigValue<Integer> computerSpaceLimit;
    private static final ConfigValue<Integer> floppySpaceLimit;
    private static final ConfigValue<Integer> maximumFilesOpen;
    private static final ConfigValue<Boolean> disableLua51Features;
    private static final ConfigValue<String> defaultComputerSettings;
    private static final ConfigValue<Boolean> debugEnabled;
    private static final ConfigValue<Boolean> logComputerErrors;

    private static final ConfigValue<Integer> computerThreads;
    private static final ConfigValue<Integer> maxMainGlobalTime;
    private static final ConfigValue<Integer> maxMainComputerTime;

    private static final ConfigValue<Boolean> httpEnabled;
    private static final ConfigValue<Boolean> httpWebsocketEnabled;
    private static final ConfigValue<List<? extends UnmodifiableConfig>> httpRules;

    private static final ConfigValue<Integer> httpMaxRequests;
    private static final ConfigValue<Integer> httpMaxWebsockets;

    private static final ConfigValue<Boolean> commandBlockEnabled;
    private static final ConfigValue<Integer> modemRange;
    private static final ConfigValue<Integer> modemHighAltitudeRange;
    private static final ConfigValue<Integer> modemRangeDuringStorm;
    private static final ConfigValue<Integer> modemHighAltitudeRangeDuringStorm;
    private static final ConfigValue<Integer> maxNotesPerTick;

    private static final ConfigValue<Boolean> turtlesNeedFuel;
    private static final ConfigValue<Integer> turtleFuelLimit;
    private static final ConfigValue<Integer> advancedTurtleFuelLimit;
    private static final ConfigValue<Boolean> turtlesObeyBlockProtection;
    private static final ConfigValue<Boolean> turtlesCanPush;
    private static final ConfigValue<List<? extends String>> turtleDisabledActions;

    private static final ConfigValue<MonitorRenderer> monitorRenderer;

    private static final ForgeConfigSpec serverSpec;
    private static final ForgeConfigSpec clientSpec;

    private Config() {}

    static
    {
        Builder builder = new Builder();

        { // General computers
            computerSpaceLimit = builder
                .comment( "The disk space limit for computers and turtles, in bytes" )
                .translation( TRANSLATION_PREFIX + "computer_space_limit" )
                .define( "computer_space_limit", ComputerCraft.computerSpaceLimit );

            floppySpaceLimit = builder
                .comment( "The disk space limit for floppy disks, in bytes" )
                .translation( TRANSLATION_PREFIX + "floppy_space_limit" )
                .define( "floppy_space_limit", ComputerCraft.floppySpaceLimit );

            maximumFilesOpen = builder
                .comment( "Set how many files a computer can have open at the same time. Set to 0 for unlimited." )
                .translation( TRANSLATION_PREFIX + "maximum_open_files" )
                .defineInRange( "maximum_open_files", ComputerCraft.maximumFilesOpen, 0, Integer.MAX_VALUE );

            disableLua51Features = builder
                .comment( "Set this to true to disable Lua 5.1 functions that will be removed in a future update. " +
                    "Useful for ensuring forward compatibility of your programs now." )
                .define( "disable_lua51_features", ComputerCraft.disableLua51Features );

            defaultComputerSettings = builder
                .comment( "A comma separated list of default system settings to set on new computers. Example: " +
                    "\"shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false\" will disable all " +
                    "autocompletion" )
                .define( "default_computer_settings", ComputerCraft.defaultComputerSettings );

            debugEnabled = builder
                .comment( "Enable Lua's debug library. This is sandboxed to each computer, so is generally safe to be used by players." )
                .define( "debug_enabled", ComputerCraft.debugEnable );

            logComputerErrors = builder
                .comment( "Log exceptions thrown by peripherals and other Lua objects.\n" +
                    "This makes it easier for mod authors to debug problems, but may result in log spam should people use buggy methods." )
                .define( "log_computer_errors", ComputerCraft.logComputerErrors );
        }

        {
            builder.comment( "Controls execution behaviour of computers. This is largely intended for fine-tuning " +
                "servers, and generally shouldn't need to be touched" );
            builder.push( "execution" );

            computerThreads = builder
                .comment( "Set the number of threads computers can run on. A higher number means more computers can run " +
                    "at once, but may induce lag.\n" +
                    "Please note that some mods may not work with a thread count higher than 1. Use with caution." )
                .worldRestart()
                .defineInRange( "computer_threads", ComputerCraft.computerThreads, 1, Integer.MAX_VALUE );

            maxMainGlobalTime = builder
                .comment( "The maximum time that can be spent executing tasks in a single tick, in milliseconds.\n" +
                    "Note, we will quite possibly go over this limit, as there's no way to tell how long a will take " +
                    "- this aims to be the upper bound of the average time." )
                .defineInRange( "max_main_global_time", (int) TimeUnit.NANOSECONDS.toMillis( ComputerCraft.maxMainGlobalTime ), 1, Integer.MAX_VALUE );

            maxMainComputerTime = builder
                .comment( "The ideal maximum time a computer can execute for in a tick, in milliseconds.\n" +
                    "Note, we will quite possibly go over this limit, as there's no way to tell how long a will take " +
                    "- this aims to be the upper bound of the average time." )
                .defineInRange( "max_main_computer_time", (int) TimeUnit.NANOSECONDS.toMillis( ComputerCraft.maxMainComputerTime ), 1, Integer.MAX_VALUE );

            builder.pop();
        }

        { // HTTP
            builder.comment( "Controls the HTTP API" );
            builder.push( "http" );

            httpEnabled = builder
                .comment( "Enable the \"http\" API on Computers (see \"rules\" for more fine grained control than this)." )
                .define( "enabled", ComputerCraft.httpEnabled );

            httpWebsocketEnabled = builder
                .comment( "Enable use of http websockets. This requires the \"http_enable\" option to also be true." )
                .define( "websocket_enabled", ComputerCraft.httpWebsocketEnabled );

            httpRules = builder
                .comment( "A list of rules which control behaviour of the \"http\" API for specific domains or IPs.\n" +
                    "Each rule is an item with a 'host' to match against, and a series of properties. " +
                    "The host may be a domain name (\"pastebin.com\"),\n" +
                    "wildcard (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\"). If no rules, the domain is blocked." )
                .defineList( "rules",
                    Stream.concat(
                        Stream.of( ComputerCraft.DEFAULT_HTTP_DENY ).map( x -> AddressRuleConfig.makeRule( x, Action.DENY ) ),
                        Stream.of( ComputerCraft.DEFAULT_HTTP_ALLOW ).map( x -> AddressRuleConfig.makeRule( x, Action.ALLOW ) )
                    ).collect( Collectors.toList() ),
                    x -> x instanceof UnmodifiableConfig && AddressRuleConfig.checkRule( (UnmodifiableConfig) x ) );

            httpMaxRequests = builder
                .comment( "The number of http requests a computer can make at one time. Additional requests will be queued, and sent when the running requests have finished. Set to 0 for unlimited." )
                .defineInRange( "max_requests", ComputerCraft.httpMaxRequests, 0, Integer.MAX_VALUE );

            httpMaxWebsockets = builder
                .comment( "The number of websockets a computer can have open at one time. Set to 0 for unlimited." )
                .defineInRange( "max_websockets", ComputerCraft.httpMaxWebsockets, 1, Integer.MAX_VALUE );

            builder.pop();
        }

        { // Peripherals
            builder.comment( "Various options relating to peripherals." );
            builder.push( "peripheral" );

            commandBlockEnabled = builder
                .comment( "Enable Command Block peripheral support" )
                .define( "command_block_enabled", ComputerCraft.enableCommandBlock );

            modemRange = builder
                .comment( "The range of Wireless Modems at low altitude in clear weather, in meters" )
                .defineInRange( "modem_range", ComputerCraft.modemRange, 0, MODEM_MAX_RANGE );

            modemHighAltitudeRange = builder
                .comment( "The range of Wireless Modems at maximum altitude in clear weather, in meters" )
                .defineInRange( "modem_high_altitude_range", ComputerCraft.modemHighAltitudeRange, 0, MODEM_MAX_RANGE );

            modemRangeDuringStorm = builder
                .comment( "The range of Wireless Modems at low altitude in stormy weather, in meters" )
                .defineInRange( "modem_range_during_storm", ComputerCraft.modemRangeDuringStorm, 0, MODEM_MAX_RANGE );

            modemHighAltitudeRangeDuringStorm = builder
                .comment( "The range of Wireless Modems at maximum altitude in stormy weather, in meters" )
                .defineInRange( "modem_high_altitude_range_during_storm", ComputerCraft.modemHighAltitudeRangeDuringStorm, 0, MODEM_MAX_RANGE );

            maxNotesPerTick = builder
                .comment( "Maximum amount of notes a speaker can play at once" )
                .defineInRange( "max_notes_per_tick", ComputerCraft.maxNotesPerTick, 1, Integer.MAX_VALUE );

            builder.pop();
        }

        { // Turtles
            builder.comment( "Various options relating to turtles." );
            builder.push( "turtle" );

            turtlesNeedFuel = builder
                .comment( "Set whether Turtles require fuel to move" )
                .define( "need_fuel", ComputerCraft.turtlesNeedFuel );

            turtleFuelLimit = builder
                .comment( "The fuel limit for Turtles" )
                .defineInRange( "normal_fuel_limit", ComputerCraft.turtleFuelLimit, 0, Integer.MAX_VALUE );

            advancedTurtleFuelLimit = builder
                .comment( "The fuel limit for Advanced Turtles" )
                .defineInRange( "advanced_fuel_limit", ComputerCraft.advancedTurtleFuelLimit, 0, Integer.MAX_VALUE );

            turtlesObeyBlockProtection = builder
                .comment( "If set to true, Turtles will be unable to build, dig, or enter protected areas (such as near the server spawn point)" )
                .define( "obey_block_protection", ComputerCraft.turtlesObeyBlockProtection );

            turtlesCanPush = builder
                .comment( "If set to true, Turtles will push entities out of the way instead of stopping if there is space to do so" )
                .define( "can_push", ComputerCraft.turtlesCanPush );

            turtleDisabledActions = builder
                .comment( "A list of turtle actions which are disabled." )
                .defineList( "disabled_actions", Collections.emptyList(), x -> x instanceof String && getAction( (String) x ) != null );

            builder.pop();
        }

        serverSpec = builder.build();

        Builder clientBuilder = new Builder();
        monitorRenderer = clientBuilder
            .comment( "The renderer to use for monitors. Generally this should be kept at \"best\" - if " +
                "monitors have performance issues, you may wish to experiment with alternative renderers." )
            .defineEnum( "monitor_renderer", MonitorRenderer.BEST );
        clientSpec = clientBuilder.build();
    }

    public static void load()
    {
        ModLoadingContext.get().registerConfig( ModConfig.Type.SERVER, serverSpec );
        ModLoadingContext.get().registerConfig( ModConfig.Type.CLIENT, clientSpec );
    }

    public static void sync()
    {
        // General
        ComputerCraft.computerSpaceLimit = computerSpaceLimit.get();
        ComputerCraft.floppySpaceLimit = floppySpaceLimit.get();
        ComputerCraft.maximumFilesOpen = maximumFilesOpen.get();
        ComputerCraft.disableLua51Features = disableLua51Features.get();
        ComputerCraft.defaultComputerSettings = defaultComputerSettings.get();
        ComputerCraft.debugEnable = debugEnabled.get();
        ComputerCraft.computerThreads = computerThreads.get();
        ComputerCraft.logComputerErrors = logComputerErrors.get();

        // Execution
        ComputerCraft.computerThreads = computerThreads.get();
        ComputerCraft.maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos( maxMainGlobalTime.get() );
        ComputerCraft.maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos( maxMainComputerTime.get() );

        // HTTP
        ComputerCraft.httpEnabled = httpEnabled.get();
        ComputerCraft.httpWebsocketEnabled = httpWebsocketEnabled.get();
        ComputerCraft.httpRules = Collections.unmodifiableList( httpRules.get().stream()
            .map( AddressRuleConfig::parseRule ).filter( Objects::nonNull ).collect( Collectors.toList() ) );

        ComputerCraft.httpMaxRequests = httpMaxRequests.get();
        ComputerCraft.httpMaxWebsockets = httpMaxWebsockets.get();

        // Peripheral
        ComputerCraft.enableCommandBlock = commandBlockEnabled.get();
        ComputerCraft.maxNotesPerTick = maxNotesPerTick.get();
        ComputerCraft.modemRange = modemRange.get();
        ComputerCraft.modemHighAltitudeRange = modemHighAltitudeRange.get();
        ComputerCraft.modemRangeDuringStorm = modemRangeDuringStorm.get();
        ComputerCraft.modemHighAltitudeRangeDuringStorm = modemHighAltitudeRangeDuringStorm.get();

        // Turtles
        ComputerCraft.turtlesNeedFuel = turtlesNeedFuel.get();
        ComputerCraft.turtleFuelLimit = turtleFuelLimit.get();
        ComputerCraft.advancedTurtleFuelLimit = advancedTurtleFuelLimit.get();
        ComputerCraft.turtlesObeyBlockProtection = turtlesObeyBlockProtection.get();
        ComputerCraft.turtlesCanPush = turtlesCanPush.get();

        ComputerCraft.turtleDisabledActions.clear();
        for( String value : turtleDisabledActions.get() ) ComputerCraft.turtleDisabledActions.add( getAction( value ) );

        // Client
        ComputerCraft.monitorRenderer = monitorRenderer.get();
    }

    @SubscribeEvent
    public static void sync( ModConfig.Loading event )
    {
        sync();
    }

    @SubscribeEvent
    public static void sync( ModConfig.Reloading event )
    {
        // Ensure file configs are reloaded. Forge should probably do this, so worth checking in the future.
        CommentedConfig config = event.getConfig().getConfigData();
        if( config instanceof CommentedFileConfig ) ((CommentedFileConfig) config).load();

        sync();
    }

    private static final Converter<String, String> converter = CaseFormat.LOWER_CAMEL.converterTo( CaseFormat.UPPER_UNDERSCORE );

    private static TurtleAction getAction( String value )
    {
        try
        {
            return TurtleAction.valueOf( converter.convert( value ) );
        }
        catch( IllegalArgumentException e )
        {
            return null;
        }
    }
}
