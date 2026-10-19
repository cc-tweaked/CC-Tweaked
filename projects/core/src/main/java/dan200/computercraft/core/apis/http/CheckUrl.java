// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import dan200.computercraft.core.apis.IAPIEnvironment;

import java.util.concurrent.Future;

/**
 * Checks a URL using {@link NetworkUtils#checkUrl(HttpUri)}.
 * <p>
 * This requires a DNS lookup, and so needs to occur off-thread.
 */
public final class CheckUrl extends Resource<CheckUrl, CheckUrl.State> {
    private static final String EVENT = "http_check";

    private final IAPIEnvironment environment;
    private final NetworkUtils network;
    private final String address;

    public CheckUrl(ResourceGroup<CheckUrl> limiter, IAPIEnvironment environment, NetworkUtils network, String address, HttpUri uri) {
        super(limiter, new Init(uri));
        this.environment = environment;
        this.network = network;
        this.address = address;
    }

    public sealed interface State extends ResourceState {
    }

    private record Init(HttpUri uri) implements State {
        @Override
        public void close() {
        }
    }

    private record Checking(Future<?> future) implements State {
        @Override
        public void close() {
            future().cancel(true);
        }
    }

    @Override
    public void start() {
        transformStateAsync(Init.class, (q, f) -> new Checking(f), this::doRun);
    }

    private StateTransition<State> doRun(Init init) {
        var uri = init.uri();
        try {
            network.checkUrl(uri);
            return StateTransition.close(() -> environment.queueEvent(EVENT, address, true));
        } catch (HTTPRequestException e) {
            return StateTransition.close(() -> environment.queueEvent(EVENT, address, false, e.getMessage()));
        }
    }
}
