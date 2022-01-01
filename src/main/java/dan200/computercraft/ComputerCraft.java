/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft;

import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.shared.Config;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.core.ClientComputerRegistry;
import dan200.computercraft.shared.computer.core.ServerComputerRegistry;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Mod( ComputerCraft.MOD_ID )
public final class ComputerCraft
{
    public static final String MOD_ID = "computercraft";

    public static int computerSpaceLimit = 1000 * 1000;
    public static int floppySpaceLimit = 125 * 1000;
    public static int maximumFilesOpen = 128;
    public static boolean disableLua51Features = false;
    public static String defaultComputerSettings = "";
    public static boolean logComputerErrors = true;
    public static boolean commandRequireCreative = true;

    public static int computerThreads = 1;
    public static long maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos( 10 );
    public static long maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos( 5 );

    public static boolean httpEnabled = true;
    public static boolean httpWebsocketEnabled = true;
    public static List<AddressRule> httpRules = List.of(
        AddressRule.parse( "$private", null, Action.DENY.toPartial() ),
        AddressRule.parse( "*", null, Action.ALLOW.toPartial() )
    );

    public static int httpMaxRequests = 16;
    public static int httpMaxWebsockets = 4;
    public static int httpDownloadBandwidth = 32 * 1024 * 1024;
    public static int httpUploadBandwidth = 32 * 1024 * 1024;

    public static boolean enableCommandBlock = false;
    public static int modemRange = 64;
    public static int modemHighAltitudeRange = 384;
    public static int modemRangeDuringStorm = 64;
    public static int modemHighAltitudeRangeDuringStorm = 384;
    public static int maxNotesPerTick = 8;
    public static MonitorRenderer monitorRenderer = MonitorRenderer.BEST;
    public static int monitorDistance = 65;
    public static long monitorBandwidth = 1_000_000;

    public static boolean turtlesNeedFuel = true;
    public static int turtleFuelLimit = 20000;
    public static int advancedTurtleFuelLimit = 100000;
    public static boolean turtlesObeyBlockProtection = true;
    public static boolean turtlesCanPush = true;

    public static int computerTermWidth = 51;
    public static int computerTermHeight = 19;

    public static final int turtleTermWidth = 39;
    public static final int turtleTermHeight = 13;

    public static int pocketTermWidth = 26;
    public static int pocketTermHeight = 20;

    public static int monitorWidth = 8;
    public static int monitorHeight = 6;

    // Registries
    public static final ClientComputerRegistry clientComputerRegistry = new ClientComputerRegistry();
    public static final ServerComputerRegistry serverComputerRegistry = new ServerComputerRegistry();

    // Logging
    public static final Logger log = LogManager.getLogger( MOD_ID );

    public ComputerCraft()
    {
        Config.setup();
        Registry.setup();
    }
}
