/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
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

import static dan200.computercraft.ComputerCraft.DEFAULT_HTTP_BLACKLIST;
import static dan200.computercraft.ComputerCraft.DEFAULT_HTTP_WHITELIST;

public class Config
{
    private static final int MODEM_MAX_RANGE = 100000;

    private static final String CATEGORY_GENERAL = "general";
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
    private static Property computerThreads;
    private static Property logComputerErrors;

    private static Property httpEnable;
    private static Property httpWebsocketEnable;
    private static Property httpWhitelist;
    private static Property httpBlacklist;

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

    public static void load( File configFile )
    {
        config = new Configuration( configFile );

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
            disableLua51Features.setComment( "Set this to true to disable Lua 5.1 functions that will be removed in a future update. Useful for ensuring forward compatibility of your programs now." );

            defaultComputerSettings = config.get( CATEGORY_GENERAL, "default_computer_settings", ComputerCraft.default_computer_settings );
            defaultComputerSettings.setComment( "A comma seperated list of default system settings to set on new computers. Example: \"shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false\" will disable all autocompletion" );

            debugEnabled = config.get( CATEGORY_GENERAL, "debug_enabled", ComputerCraft.debug_enable );
            debugEnabled.setComment( "Enable Lua's debug library. This is sandboxed to each computer, so is generally safe to be used by players." );

            computerThreads = config.get( CATEGORY_GENERAL, "computer_threads", ComputerCraft.computer_threads );
            computerThreads
                .setMinValue( 1 )
                .setRequiresMcRestart( true )
                .setComment( "Set the number of threads computers can run on. A higher number means more computers can run at once, but may induce lag.\n" +
                    "Please note that some mods may not work with a thread count higher than 1. Use with caution." );

            logComputerErrors = config.get( CATEGORY_GENERAL, "log_computer_errors", ComputerCraft.logPeripheralErrors );
            logComputerErrors.setComment( "Log exceptions thrown by peripherals and other Lua objects.\n" +
                "This makes it easier for mod authors to debug problems, but may result in log spam should people use buggy methods." );

            setOrder(
                CATEGORY_GENERAL,
                computerSpaceLimit, floppySpaceLimit, maximumFilesOpen,
                disableLua51Features, defaultComputerSettings, debugEnabled, computerThreads, logComputerErrors
            );
        }

        { // HTTP
            renameProperty( CATEGORY_GENERAL, "http_enable", CATEGORY_HTTP, "enabled" );
            renameProperty( CATEGORY_GENERAL, "http_websocket_enable", CATEGORY_HTTP, "websocket_enabled" );
            renameProperty( CATEGORY_GENERAL, "http_whitelist", CATEGORY_HTTP, "whitelist" );
            renameProperty( CATEGORY_GENERAL, "http_blacklist", CATEGORY_HTTP, "blacklist" );

            httpEnable = config.get( CATEGORY_HTTP, "enabled", ComputerCraft.http_enable );
            httpEnable.setComment( "Enable the \"http\" API on Computers (see \"http_whitelist\" and \"http_blacklist\" for more fine grained control than this)" );

            httpWebsocketEnable = config.get( CATEGORY_HTTP, "websocket_enabled", ComputerCraft.http_websocket_enable );
            httpWebsocketEnable.setComment( "Enable use of http websockets. This requires the \"http_enable\" option to also be true." );

            httpWhitelist = config.get( CATEGORY_HTTP, "whitelist", DEFAULT_HTTP_WHITELIST );
            httpWhitelist.setComment( "A list of wildcards for domains or IP ranges that can be accessed through the \"http\" API on Computers.\n" +
                "Set this to \"*\" to access to the entire internet. Example: \"*.pastebin.com\" will restrict access to just subdomains of pastebin.com.\n" +
                "You can use domain names (\"pastebin.com\"), wilcards (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\")." );

            httpBlacklist = config.get( CATEGORY_HTTP, "blacklist", DEFAULT_HTTP_BLACKLIST );
            httpBlacklist.setComment( "A list of wildcards for domains or IP ranges that cannot be accessed through the \"http\" API on Computers.\n" +
                "If this is empty then all whitelisted domains will be accessible. Example: \"*.github.com\" will block access to all subdomains of github.com.\n" +
                "You can use domain names (\"pastebin.com\"), wilcards (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\")." );

            httpTimeout = config.get( CATEGORY_HTTP, "timeout", ComputerCraft.httpTimeout );
            httpTimeout.setComment( "The period of time (in milliseconds) to wait before a HTTP request times out. Set to 0 for unlimited." );
            httpTimeout.setMinValue( 0 );

            httpMaxRequests = config.get( CATEGORY_HTTP, "max_requests", ComputerCraft.httpMaxRequests );
            httpMaxRequests.setComment( "The number of http requests a computer can make at one time. Additional requests will be queued, and sent when the running requests have finished. Set to 0 for unlimited." );
            httpMaxRequests.setMinValue( 0 );

            httpMaxDownload = config.get( CATEGORY_HTTP, "max_download", (int) ComputerCraft.httpMaxDownload );
            httpMaxDownload.setComment( "The maximum size (in bytes) that a computer can download in a single request. Note that responses may receive more data than allowed, but this data will not be returned to the client." );
            httpMaxDownload.setMinValue( 0 );

            httpMaxUpload = config.get( CATEGORY_HTTP, "max_upload", (int) ComputerCraft.httpMaxUpload );
            httpMaxUpload.setComment( "The maximum size (in bytes) that a computer can upload in a single request. This includes headers and POST text." );
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
                httpEnable, httpWebsocketEnable, httpWhitelist, httpBlacklist,
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

            turtlesNeedFuel = config.get( CATEGORY_TURTLE, "need_fuel", ComputerCraft.turtlesNeedFuel );
            turtlesNeedFuel.setComment( "Set whether Turtles require fuel to move" );

            turtleFuelLimit = config.get( CATEGORY_TURTLE, "normal_fuel_limit", ComputerCraft.turtleFuelLimit );
            turtleFuelLimit.setComment( "The fuel limit for Turtles" );
            turtleFuelLimit.setMinValue( 0 );

            advancedTurtleFuelLimit = config.get( CATEGORY_TURTLE, "advanced_fuel_limit", ComputerCraft.advancedTurtleFuelLimit );
            advancedTurtleFuelLimit.setComment( "The fuel limit for Advanced Turtles" );
            advancedTurtleFuelLimit.setMinValue( 0 );

            turtlesObeyBlockProtection = config.get( CATEGORY_TURTLE, "obey_block_protection", ComputerCraft.turtlesObeyBlockProtection );
            turtlesObeyBlockProtection.setComment( "If set to true, Turtles will be unable to build, dig, or enter protected areas (such as near the server spawn point)" );

            turtlesCanPush = config.get( CATEGORY_TURTLE, "can_push", ComputerCraft.turtlesCanPush );
            turtlesCanPush.setComment( "If set to true, Turtles will push entities out of the way instead of stopping if there is space to do so" );

            turtleDisabledActions = config.get( CATEGORY_TURTLE, "disabled_actions", new String[0] );
            turtleDisabledActions.setComment( "A list of turtle actions which are disabled." );

            setOrder(
                CATEGORY_TURTLE,
                turtlesNeedFuel, turtleFuelLimit, advancedTurtleFuelLimit, turtlesObeyBlockProtection, turtlesCanPush, turtleDisabledActions
            );
        }

        setupLanguage( config.getCategory( CATEGORY_GENERAL ), "gui.computercraft:config" );
        for( String child : config.getCategoryNames() )
        {
            if( child.equals( CATEGORY_GENERAL ) ) continue;
            setupLanguage( config.getCategory( child ), "gui.computercraft:config." + child );
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
        config.load();
        sync();
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
        ComputerCraft.computer_threads = computerThreads.getInt();
        ComputerCraft.logPeripheralErrors = logComputerErrors.getBoolean();

        // HTTP
        ComputerCraft.http_enable = httpEnable.getBoolean();
        ComputerCraft.http_websocket_enable = httpWebsocketEnable.getBoolean();
        ComputerCraft.http_whitelist = new AddressPredicate( httpWhitelist.getStringList() );
        ComputerCraft.http_blacklist = new AddressPredicate( httpBlacklist.getStringList() );

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
