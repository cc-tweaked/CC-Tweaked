// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core;

import dan200.computercraft.core.apis.http.options.AddressRule;

/**
 * Replaces {@link CoreConfig} with a slightly cut-down version.
 * <p>
 * This is mostly required to avoid pulling in {@link AddressRule}.
 */
public final class TCoreConfig {
    private TCoreConfig() {
    }

    public static int maximumFilesOpen = 128;
    public static String defaultComputerSettings = "";

    public static boolean httpEnabled = true;
    public static boolean httpWebsocketEnabled = true;
    public static int httpMaxRequests = 16;
    public static int httpMaxWebsockets = 4;
}
