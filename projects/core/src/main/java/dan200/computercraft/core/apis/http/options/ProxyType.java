// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

/**
 * The type of proxy to use for HTTP requests.
 *
 * @see dan200.computercraft.core.apis.http.NetworkUtils#getProxyHandler(Options, int)
 * @see dan200.computercraft.core.CoreConfig#httpProxyType
 */
public enum ProxyType {
    HTTP,
    HTTPS,
    SOCKS4,
    SOCKS5
}
