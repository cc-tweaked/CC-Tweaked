// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import dan200.computercraft.core.apis.http.NettyHttp;

/**
 * The type of proxy to use for HTTP requests.
 *
 * @see dan200.computercraft.core.apis.http.NetworkUtils#getProxyHandler(Options, int)
 * @see NettyHttp.ProxyConfig#type()
 */
public enum ProxyType {
    HTTP,
    HTTPS,
    SOCKS4,
    SOCKS5
}
