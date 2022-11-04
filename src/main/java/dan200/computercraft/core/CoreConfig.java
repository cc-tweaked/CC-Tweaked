/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core;

import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;

import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

/**
 * Config options for ComputerCraft's Lua runtime.
 */
public final class CoreConfig {
    // TODO: Ideally this would be an instance in {@link ComputerContext}, but sharing this everywhere it needs to be is
    //  tricky.

    private CoreConfig() {
    }

    public static int maximumFilesOpen = 128;
    public static boolean disableLua51Features = false;
    public static String defaultComputerSettings = "";

    public static long maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos(10);
    public static long maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos(5);

    public static boolean httpEnabled = true;
    public static boolean httpWebsocketEnabled = true;
    public static List<AddressRule> httpRules = List.of(
        AddressRule.parse("$private", OptionalInt.empty(), Action.DENY.toPartial()),
        AddressRule.parse("*", OptionalInt.empty(), Action.ALLOW.toPartial())
    );
    public static int httpMaxRequests = 16;
    public static int httpMaxWebsockets = 4;
    public static int httpDownloadBandwidth = 32 * 1024 * 1024;
    public static int httpUploadBandwidth = 32 * 1024 * 1024;
}
