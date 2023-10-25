// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core;

import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.core.apis.http.options.ProxyType;

import java.util.List;
import java.util.OptionalInt;

/**
 * Config options for ComputerCraft's Lua runtime.
 */
public final class CoreConfig {
    // TODO: Ideally this would be an instance in {@link ComputerContext}, but sharing this everywhere it needs to be is
    //  tricky.

    private CoreConfig() {
    }

    public static int maximumFilesOpen = 128;
    public static String defaultComputerSettings = "";

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
    public static ProxyType httpProxyType = ProxyType.HTTP;
    public static String httpProxyHost = "";
    public static int httpProxyPort = 8080;
    public static String httpProxyUsername = "";
    public static String httpProxyPassword = "";
}
