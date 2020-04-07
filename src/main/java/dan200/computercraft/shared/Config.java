/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.core.apis.AddressPredicate;
import dan200.computercraft.core.apis.http.websocket.Websocket;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static dan200.computercraft.ComputerCraft.DEFAULT_HTTP_BLACKLIST;
import static dan200.computercraft.ComputerCraft.DEFAULT_HTTP_WHITELIST;

public final class Config
{
    private static final int MODEM_MAX_RANGE = 100000;

    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_EXECUTION = "execution";
    private static final String CATEGORY_HTTP = "http";
    private static final String CATEGORY_PERIPHERAL = "peripheral";
    private static final String CATEGORY_TURTLE = "turtle";

    private static Configuration config;

    private static Property computerSpaceLimit;
    private static Property floppySpaceLimit;
    private static Property maximumFilesOpen;
    private static Property disableLua51Features;
    private static Property defaultComputerSettings;
    private static Property debugEnabled;
    private static Property logComputerErrors;

    private static Property computerThreads;
    private static Property maxMainGlobalTime;
    private static Property maxMainComputerTime;

    private static Property httpEnable;
    private static Property httpWebsocketEnable;
    private static Property httpAllowedDomains;
    private static Property httpBlockedDomains;

    private static Property httpTimeout;
    private static Property httpMaxRequests;
    private static Property httpMaxDownload;
    private static Property httpMaxUpload;
    private static Property httpMaxWebsockets;
    private static Property httpMaxWebsocketMessage;

    private static Property commandBlockEnabled;
    private static Property modemRange;
    private static Property modemHighAltitudeRange;
    private static Property modemRangeDuringStorm;
    private static Property modemHighAltitudeRangeDuringStorm;
    private static Property maxNotesPerTick;

    private static Property turtlesNeedFuel;
    private static Property turtleFuelLimit;
    private static Property advancedTurtleFuelLimit;
    private static Property turtlesObeyBlockProtection;
    private static Property turtlesCanPush;
    private static Property turtleDisabledActions;
    private static Property turtleAnimationDuration;

    private Config() {}

    public static void load( File configFile )
    {
        config = new Configuration( configFile, ComputerCraft.getVersion() );

        config.load();

        { // General computers
            renameProperty( CATEGORY_GENERAL, "computerSpaceLimit", CATEGORY_GENERAL, "computer_space_limit" );
            renameProperty( CATEGORY_GENERAL, "floppySpaceLimit", CATEGORY_GENERAL, "floppy_space_limit" );
            renameProperty( CATEGORY_GENERAL, "maximumFilesOpen", CATEGORY_GENERAL, "maximum_open_files" );
            renameProperty( CATEGORY_GENERAL, "debug_enable", CATEGORY_GENERAL, "debug_enabled" );
            renameProperty( CATEGORY_GENERAL, "logPeripheralErrors", CATEGORY_GENERAL, "log_computer_errors" );

            computerSpaceLimit = config.get( CATEGORY_GENERAL, "computer_space_limit", ComputerCraft.computerSpaceLimit );
            computerSpaceLimit.setComment( "The disk space limit for computers and turtles, in bytes" );

            floppySpaceLimit = config.get( CATEGORY_GENERAL, "floppy_space_limit", ComputerCraft.floppySpaceLimit );
            floppySpaceLimit.setComment( "The disk space limit for floppy disks, in bytes" );

            maximumFilesOpen = config.get( CATEGORY_GENERAL, "maximum_open_files", ComputerCraft.maximumFilesOpen );
            maximumFilesOpen.setComment( "Set how many files a computer can have open at the same time. Set to 0 for unlimited." );
            maximumFilesOpen.setMinValue( 0 );

            disableLua51Features = config.get( CATEGORY_GENERAL, "disable_lua51_features", ComputerCraft.disable_lua51_features );
            disableLua51Features.setComment( "Set this to true to disable Lua 5.1 functions that will be removed in a future " +
                "update. Useful for ensuring forward compatibility of your programs now." );

            defaultComputerSettings = config.get( CATEGORY_GENERAL, "default_computer_settings", ComputerCraft.default_computer_settings );
            defaultComputerSettings.setComment( "A comma separated list of default system settings to set on new computers. Example: " +
                "\"shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false\" will disable all autocompletion" );

            debugEnabled = config.get( CATEGORY_GENERAL, "debug_enabled", ComputerCraft.debug_enable );
            debugEnabled.setComment( "Enable Lua's debug library. This is sandboxed to each computer, so is generally safe to be used by players." );

            logComputerErrors = config.get( CATEGORY_GENERAL, "log_computer_errors", ComputerCraft.logPeripheralErrors );
            logComputerErrors.setComment( "Log exceptions thrown by peripherals and other Lua objects.\n" +
                "This makes it easier for mod authors to debug problems, but may result in log spam should people use buggy methods." );

            setOrder(
                CATEGORY_GENERAL,
                computerSpaceLimit, floppySpaceLimit, maximumFilesOpen,
                disableLua51Features, defaultComputerSettings, debugEnabled, logComputerErrors
            );
        }

        { // Execution
            renameProperty( CATEGORY_GENERAL, "computer_threads", CATEGORY_EXECUTION, "computer_threads" );

            config.getCategory( CATEGORY_EXECUTION )
                .setComment( "Controls execution behaviour of computers. This is largely intended for fine-tuning " +
                    "servers, and generally shouldn't need to be touched" );

            computerThreads = config.get( CATEGORY_EXECUTION, "computer_threads", ComputerCraft.computer_threads );
            computerThreads
                .setMinValue( 1 )
                .setRequiresMcRestart( true )
                .setComment( "Set the number of threads computers can run on. A higher number means more computers can " +
                    "run at once, but may induce lag.\n" +
                    "Please note that some mods may not work with a thread count higher than 1. Use with caution." );

            maxMainGlobalTime = config.get( CATEGORY_EXECUTION, "max_main_global_time", (int) TimeUnit.NANOSECONDS.toMillis( ComputerCraft.maxMainGlobalTime ) );
            maxMainGlobalTime
                .setMinValue( 1 )
                .setComment( "The maximum time that can be spent executing tasks in a single tick, in milliseconds.\n" +
                    "Note, we will quite possibly go over this limit, as there's no way to tell how long a will take - this aims " +
                    "to be the upper bound of the average time." );

            maxMainComputerTime = config.get( CATEGORY_EXECUTION, "max_main_computer_time", (int) TimeUnit.NANOSECONDS.toMillis( ComputerCraft.maxMainComputerTime ) );
            maxMainComputerTime
                .setMinValue( 1 )
                .setComment( "The ideal maximum time a computer can execute for in a tick, in milliseconds.\n" +
                    "Note, we will quite possibly go over this limit, as there's no way to tell how long a will take - this aims " +
                    "to be the upper bound of the average time." );

            setOrder(
                CATEGORY_EXECUTION,
                computerThreads, maxMainGlobalTime, maxMainComputerTime
            );
        }

        { // HTTP
            renameProperty( CATEGORY_GENERAL, "http_enable", CATEGORY_HTTP, "enabled" );
            renameProperty( CATEGORY_GENERAL, "http_websocket_enable", CATEGORY_HTTP, "websocket_enabled" );
            renameProperty( CATEGORY_GENERAL, "http_whitelist", CATEGORY_HTTP, "allowed_domains" );
            renameProperty( CATEGORY_GENERAL, "http_blacklist", CATEGORY_HTTP, "blocked_domains" );
            renameProperty( CATEGORY_HTTP, "whitelist", CATEGORY_HTTP, "allowed_domains" );
            renameProperty( CATEGORY_HTTP, "blacklist", CATEGORY_HTTP, "blocked_domains" );

            config.getCategory( CATEGORY_HTTP )
                .setComment( "Controls the HTTP API" );

            httpEnable = config.get( CATEGORY_HTTP, "enabled", ComputerCraft.http_enable );
            httpEnable.setComment( "Enable the \"http\" API on Computers (see \"allowed_domains\" and \"blocked_domains\" " +
                "for more fine grained control than this)" );

            httpWebsocketEnable = config.get( CATEGORY_HTTP, "websocket_enabled", ComputerCraft.http_websocket_enable );
            httpWebsocketEnable.setComment( "Enable use of http websockets. This requires the \"http_enable\" option to also be true." );

            httpAllowedDomains = config.get( CATEGORY_HTTP, "allowed_domains", DEFAULT_HTTP_WHITELIST );
            httpAllowedDomains.setComment( "A list of wildcards for domains or IP ranges that can be accessed through the " +
                "\"http\" API on Computers.\n" +
                "Set this to \"*\" to access to the entire internet. Example: \"*.pastebin.com\" will restrict access to " +
                "just subdomains of pastebin.com.\n" +
                "You can use domain names (\"pastebin.com\"), wildcards (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\")." );

            httpBlockedDomains = config.get( CATEGORY_HTTP, "blocked_domains", DEFAULT_HTTP_BLACKLIST );
            httpBlockedDomains.setComment( "A list of wildcards for domains or IP ranges that cannot be accessed through the " +
                "\"http\" API on Computers.\n" +
                "If this is empty then all explicitly allowed domains will be accessible. Example: \"*.github.com\" will block " +
                "access to all subdomains of github.com.\n" +
                "You can use domain names (\"pastebin.com\"), wildcards (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\")." );

            httpTimeout = config.get( CATEGORY_HTTP, "timeout", ComputerCraft.httpTimeout );
            httpTimeout.setComment( "The period of time (in milliseconds) to wait before a HTTP request times out. Set to 0 for unlimited." );
            httpTimeout.setMinValue( 0 );

            httpMaxRequests = config.get( CATEGORY_HTTP, "max_requests", ComputerCraft.httpMaxRequests );
            httpMaxRequests.setComment( "The number of http requests a computer can make at one time. Additional requests " +
                "will be queued, and sent when the running requests have finished. Set to 0 for unlimited." );
            httpMaxRequests.setMinValue( 0 );

            httpMaxDownload = config.get( CATEGORY_HTTP, "max_download", (int) ComputerCraft.httpMaxDownload );
            httpMaxDownload.setComment( "The maximum size (in bytes) that a computer can download in a single request. " +
                "Note that responses may receive more data than allowed, but this data will not be returned to the client." );
            httpMaxDownload.setMinValue( 0 );

            httpMaxUpload = config.get( CATEGORY_HTTP, "max_upload", (int) ComputerCraft.httpMaxUpload );
            httpMaxUpload.setComment( "The maximum size (in bytes) that a computer can upload in a single request. This " +
                "includes headers and POST text." );
            httpMaxUpload.setMinValue( 0 );

            httpMaxWebsockets = config.get( CATEGORY_HTTP, "max_websockets", ComputerCraft.httpMaxWebsockets );
            httpMaxWebsockets.setComment( "The number of websockets a computer can have open at one time. Set to 0 for unlimited." );
            httpMaxWebsockets.setMinValue( 1 );

            httpMaxWebsocketMessage = config.get( CATEGORY_HTTP, "max_websocket_message", ComputerCraft.httpMaxWebsocketMessage );
            httpMaxWebsocketMessage.setComment( "The maximum size (in bytes) that a computer can send or receive in one websocket packet." );
            httpMaxWebsocketMessage.setMinValue( 0 );
            httpMaxWebsocketMessage.setMaxValue( Websocket.MAX_MESSAGE_SIZE );

            setOrder(
                CATEGORY_HTTP,
                httpEnable, httpWebsocketEnable, httpAllowedDomains, httpBlockedDomains,
                httpTimeout, httpMaxRequests, httpMaxDownload, httpMaxUpload, httpMaxWebsockets, httpMaxWebsocketMessage
            );
        }

        { // Peripherals
            renameProperty( CATEGORY_GENERAL, "enableCommandBlock", CATEGORY_PERIPHERAL, "command_block_enabled" );
            renameProperty( CATEGORY_GENERAL, "modem_range", CATEGORY_PERIPHERAL, "modem_range" );
            renameProperty( CATEGORY_GENERAL, "modem_highAltitudeRange", CATEGORY_PERIPHERAL, "modem_high_altitude_range" );
            renameProperty( CATEGORY_GENERAL, "modem_rangeDuringStorm", CATEGORY_PERIPHERAL, "modem_range_during_storm" );
            renameProperty( CATEGORY_GENERAL, "modem_highAltitudeRangeDuringStorm", CATEGORY_PERIPHERAL, "modem_high_altitude_range_during_storm" );
            renameProperty( CATEGORY_GENERAL, "maxNotesPerTick", CATEGORY_PERIPHERAL, "max_notes_per_tick" );

            config.getCategory( CATEGORY_PERIPHERAL )
                .setComment( "Various options relating to peripherals." );

            commandBlockEnabled = config.get( CATEGORY_PERIPHERAL, "command_block_enabled", ComputerCraft.enableCommandBlock );
            commandBlockEnabled.setComment( "Enable Command Block peripheral support" );

            modemRange = config.get( CATEGORY_PERIPHERAL, "modem_range", ComputerCraft.modem_range );
            modemRange.setComment( "The range of Wireless Modems at low altitude in clear weather, in meters" );
            modemRange.setMinValue( 0 );
            modemRange.setMaxValue( MODEM_MAX_RANGE );

            modemHighAltitudeRange = config.get( CATEGORY_PERIPHERAL, "modem_high_altitude_range", ComputerCraft.modem_highAltitudeRange );
            modemHighAltitudeRange.setComment( "The range of Wireless Modems at maximum altitude in clear weather, in meters" );
            modemHighAltitudeRange.setMinValue( 0 );
            modemHighAltitudeRange.setMaxValue( MODEM_MAX_RANGE );

            modemRangeDuringStorm = config.get( CATEGORY_PERIPHERAL, "modem_range_during_storm", ComputerCraft.modem_rangeDuringStorm );
            modemRangeDuringStorm.setComment( "The range of Wireless Modems at low altitude in stormy weather, in meters" );
            modemRangeDuringStorm.setMinValue( 0 );
            modemRangeDuringStorm.setMaxValue( MODEM_MAX_RANGE );

            modemHighAltitudeRangeDuringStorm = config.get( CATEGORY_PERIPHERAL, "modem_high_altitude_range_during_storm", ComputerCraft.modem_highAltitudeRangeDuringStorm );
            modemHighAltitudeRangeDuringStorm.setComment( "The range of Wireless Modems at maximum altitude in stormy weather, in meters" );
            modemHighAltitudeRangeDuringStorm.setMinValue( 0 );
            modemHighAltitudeRangeDuringStorm.setMaxValue( MODEM_MAX_RANGE );

            maxNotesPerTick = config.get( CATEGORY_PERIPHERAL, "max_notes_per_tick", ComputerCraft.maxNotesPerTick );
            maxNotesPerTick.setComment( "Maximum amount of notes a speaker can play at once" );
            maxNotesPerTick.setMinValue( 1 );

            setOrder(
                CATEGORY_PERIPHERAL,
                commandBlockEnabled, modemRange, modemHighAltitudeRange, modemRangeDuringStorm, modemHighAltitudeRangeDuringStorm, maxNotesPerTick
            );
        }

        { // Turtles
            renameProperty( CATEGORY_GENERAL, "turtlesNeedFuel", CATEGORY_TURTLE, "need_fuel" );
            renameProperty( CATEGORY_GENERAL, "turtleFuelLimit", CATEGORY_TURTLE, "normal_fuel_limit" );
            renameProperty( CATEGORY_GENERAL, "advancedTurtleFuelLimit", CATEGORY_TURTLE, "advanced_fuel_limit" );
            renameProperty( CATEGORY_GENERAL, "turtlesObeyBlockProtection", CATEGORY_TURTLE, "obey_block_protection" );
            renameProperty( CATEGORY_GENERAL, "turtlesCanPush", CATEGORY_TURTLE, "can_push" );
            renameProperty( CATEGORY_GENERAL, "turtle_disabled_actions", CATEGORY_TURTLE, "disabled_actions" );

            config.getCategory( CATEGORY_TURTLE )
                .setComment( "Various options relating to turtles." );

            turtlesNeedFuel = config.get( CATEGORY_TURTLE, "need_fuel", ComputerCraft.turtlesNeedFuel );
            turtlesNeedFuel.setComment( "Set whether Turtles require fuel to move" );

            turtleFuelLimit = config.get( CATEGORY_TURTLE, "normal_fuel_limit", ComputerCraft.turtleFuelLimit );
            turtleFuelLimit.setComment( "The fuel limit for Turtles" );
            turtleFuelLimit.setMinValue( 0 );

            advancedTurtleFuelLimit = config.get( CATEGORY_TURTLE, "advanced_fuel_limit", ComputerCraft.advancedTurtleFuelLimit );
            advancedTurtleFuelLimit.setComment( "The fuel limit for Advanced Turtles" );
            advancedTurtleFuelLimit.setMinValue( 0 );

            turtlesObeyBlockProtection = config.get( CATEGORY_TURTLE, "obey_block_protection", ComputerCraft.turtlesObeyBlockProtection );
            turtlesObeyBlockProtection.setComment( "If set to true, Turtles will be unable to build, dig, or enter protected " +
                "areas (such as near the server spawn point)" );

            turtlesCanPush = config.get( CATEGORY_TURTLE, "can_push", ComputerCraft.turtlesCanPush );
            turtlesCanPush.setComment( "If set to true, Turtles will push entities out of the way instead of stopping if " +
                "there is space to do so" );

            turtleDisabledActions = config.get( CATEGORY_TURTLE, "disabled_actions", new String[0] );
            turtleDisabledActions.setComment( "A list of turtle actions which are disabled." );

            turtleAnimationDuration = config.get( CATEGORY_TURTLE, "animation_duration", ComputerCraft.turtleAnimationDuration );
            turtleAnimationDuration.setComment( "How fast the turtle moves and digs, lower values are faster. Some values may make turtles hard to chase." );

            setOrder(
                CATEGORY_TURTLE,
                turtlesNeedFuel, turtleFuelLimit, advancedTurtleFuelLimit, turtlesObeyBlockProtection, turtlesCanPush, turtleDisabledActions, turtleAnimationDuration
            );
        }

        for( String child : config.getCategoryNames() )
        {
            setupLanguage(
                config.getCategory( child ),
                child.equals( CATEGORY_GENERAL ) ? "gui.computercraft:config" : "gui.computercraft:config." + child
            );
        }

        sync();
    }

    private static void setOrder( String category, Property... properties )
    {
        List<String> names = new ArrayList<>( properties.length );
        for( Property property : properties ) names.add( property.getName() );
        config.getCategory( category ).setPropertyOrder( names );
    }

    private static void renameProperty( String oldCat, String oldProp, String newCat, String newProp )
    {
        if( !config.hasCategory( oldCat ) ) return;

        ConfigCategory cat = config.getCategory( oldCat );
        if( !cat.containsKey( oldProp ) ) return;

        Property prop = cat.remove( oldProp );
        prop.setName( newProp );
        config.getCategory( newCat ).put( newProp, prop );

        // Clean up old categories
        if( cat.isEmpty() ) config.removeCategory( cat );
    }

    private static void setupLanguage( ConfigCategory category, String key )
    {
        category.setLanguageKey( key );
        for( Property property : category.getOrderedValues() )
        {
            property.setLanguageKey( key + "." + property.getName() );
        }

        for( ConfigCategory child : category.getChildren() )
        {
            setupLanguage( child, key + "." + child.getName() );
        }
    }

    public static void reload()
    {
        Configuration newConfig = new Configuration( config.getConfigFile(), ComputerCraft.getVersion() );
        Set<String> oldCategories = config.getCategoryNames(), newCategories = newConfig.getCategoryNames();

        // Sync any categories on the original config
        for( String category : oldCategories )
        {
            if( newCategories.contains( category ) )
            {
                reloadCategory( config.getCategory( category ), newConfig.getCategory( category ) );
            }
            else
            {
                for( Property property : config.getCategory( category ).getValues().values() ) property.setToDefault();
            }
        }

        // And drop any unexpected ones.
        for( String category : newCategories )
        {
            if( !oldCategories.contains( category ) )
            {
                ComputerCraft.log.warn( "Cannot sync unknown config category {}", category );
            }
        }

        sync();
    }

    private static void reloadCategory( ConfigCategory oldCat, ConfigCategory newCat )
    {
        // Copy the config values across to the original config.
        for( Map.Entry<String, Property> child : newCat.getValues().entrySet() )
        {
            Property oldProperty = oldCat.get( child.getKey() ), newProperty = child.getValue();
            if( oldProperty.getType() != newProperty.getType() || oldProperty.isList() != newProperty.isList() )
            {
                ComputerCraft.log.warn(
                    "Cannot sync config property {} (type changed from {} to {})",
                    child.getKey(), getType( oldProperty ), getType( newProperty )
                );
                continue;
            }

            if( oldProperty.isList() )
            {
                oldProperty.setValues( newProperty.getStringList() );
            }
            else
            {
                oldProperty.setValue( newProperty.getString() );
            }
        }

        // Reset any missing properties.
        for( Map.Entry<String, Property> child : oldCat.getValues().entrySet() )
        {
            if( !newCat.containsKey( child.getKey() ) ) child.getValue().setToDefault();
        }
    }

    private static String getType( Property property )
    {
        return property.getType() + (property.isList() ? " list" : "");
    }

    public static void sync()
    {
        // General
        ComputerCraft.computerSpaceLimit = computerSpaceLimit.getInt();
        ComputerCraft.floppySpaceLimit = floppySpaceLimit.getInt();
        ComputerCraft.maximumFilesOpen = Math.max( 0, maximumFilesOpen.getInt() );
        ComputerCraft.disable_lua51_features = disableLua51Features.getBoolean();
        ComputerCraft.default_computer_settings = defaultComputerSettings.getString();
        ComputerCraft.debug_enable = debugEnabled.getBoolean();
        ComputerCraft.logPeripheralErrors = logComputerErrors.getBoolean();

        // Execution
        ComputerCraft.computer_threads = computerThreads.getInt();
        ComputerCraft.maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos( Math.max( 1, maxMainGlobalTime.getLong() ) );
        ComputerCraft.maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos( Math.max( 1, maxMainComputerTime.getLong() ) );

        // HTTP
        ComputerCraft.http_enable = httpEnable.getBoolean();
        ComputerCraft.http_websocket_enable = httpWebsocketEnable.getBoolean();
        ComputerCraft.http_whitelist = new AddressPredicate( httpAllowedDomains.getStringList() );
        ComputerCraft.http_blacklist = new AddressPredicate( httpBlockedDomains.getStringList() );

        ComputerCraft.httpTimeout = Math.max( 0, httpTimeout.getInt() );
        ComputerCraft.httpMaxRequests = Math.max( 1, httpMaxRequests.getInt() );
        ComputerCraft.httpMaxDownload = Math.max( 0, httpMaxDownload.getLong() );
        ComputerCraft.httpMaxUpload = Math.max( 0, httpMaxUpload.getLong() );
        ComputerCraft.httpMaxWebsockets = Math.max( 1, httpMaxWebsockets.getInt() );
        ComputerCraft.httpMaxWebsocketMessage = Math.max( 0, httpMaxWebsocketMessage.getInt() );

        // Peripheral
        ComputerCraft.enableCommandBlock = commandBlockEnabled.getBoolean();
        ComputerCraft.maxNotesPerTick = Math.max( 1, maxNotesPerTick.getInt() );
        ComputerCraft.modem_range = Math.min( modemRange.getInt(), MODEM_MAX_RANGE );
        ComputerCraft.modem_highAltitudeRange = Math.min( modemHighAltitudeRange.getInt(), MODEM_MAX_RANGE );
        ComputerCraft.modem_rangeDuringStorm = Math.min( modemRangeDuringStorm.getInt(), MODEM_MAX_RANGE );
        ComputerCraft.modem_highAltitudeRangeDuringStorm = Math.min( modemHighAltitudeRangeDuringStorm.getInt(), MODEM_MAX_RANGE );

        // Turtles
        ComputerCraft.turtlesNeedFuel = turtlesNeedFuel.getBoolean();
        ComputerCraft.turtleFuelLimit = turtleFuelLimit.getInt();
        ComputerCraft.advancedTurtleFuelLimit = advancedTurtleFuelLimit.getInt();
        ComputerCraft.turtlesObeyBlockProtection = turtlesObeyBlockProtection.getBoolean();
        ComputerCraft.turtlesCanPush = turtlesCanPush.getBoolean();

        ComputerCraft.turtleDisabledActions.clear();
        Converter<String, String> converter = CaseFormat.LOWER_CAMEL.converterTo( CaseFormat.UPPER_UNDERSCORE );
        for( String value : turtleDisabledActions.getStringList() )
        {
            try
            {
                ComputerCraft.turtleDisabledActions.add( TurtleAction.valueOf( converter.convert( value ) ) );
            }
            catch( IllegalArgumentException e )
            {
                ComputerCraft.log.error( "Unknown turtle action " + value );
            }
        }

        ComputerCraft.turtleAnimationDuration = turtleAnimationDuration.getInt();

        config.save();
    }

    public static List<IConfigElement> getConfigElements()
    {
        ArrayList<IConfigElement> elements = new ArrayList<>();

        // Add all child categories
        for( String categoryName : config.getCategoryNames() )
        {
            if( categoryName.equals( CATEGORY_GENERAL ) ) continue;
            ConfigCategory category = config.getCategory( categoryName );
            elements.add( new ConfigElement( category ) );
        }

        // Add the general category
        for( Property property : config.getCategory( CATEGORY_GENERAL ).getOrderedValues() )
        {
            elements.add( new ConfigElement( property ) );
        }

        return elements;
    }

}
