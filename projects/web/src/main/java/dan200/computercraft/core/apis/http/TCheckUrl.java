// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import dan200.computercraft.core.apis.IAPIEnvironment;

import java.net.URI;

/**
 * Replaces {@link CheckUrl} with an implementation which unconditionally returns true.
 */
public class TCheckUrl extends Resource<TCheckUrl> {
    private static final String EVENT = "http_check";

    private final IAPIEnvironment environment;
    private final String address;

    public TCheckUrl(ResourceGroup<TCheckUrl> limiter, IAPIEnvironment environment, String address, URI uri) {
        super(limiter);
        this.environment = environment;
        this.address = address;
    }

    public void run() {
        if (isClosed()) return;
        environment.queueEvent(EVENT, address, true);
    }
}
