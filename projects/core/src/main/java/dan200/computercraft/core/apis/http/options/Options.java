// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;


/**
 * Options about a specific domain.
 */
public final class Options {
    public final Action action;
    public final long maxUpload;
    public final long maxDownload;
    public final int websocketMessage;

    Options(Action action, long maxUpload, long maxDownload, int websocketMessage) {
        this.action = action;
        this.maxUpload = maxUpload;
        this.maxDownload = maxDownload;
        this.websocketMessage = websocketMessage;
    }
}
