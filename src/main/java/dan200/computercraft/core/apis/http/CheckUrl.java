/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Future;

import dan200.computercraft.core.apis.IAPIEnvironment;

/**
 * Checks a URL using {@link NetworkUtils#getAddress(String, int, boolean)}}
 *
 * This requires a DNS lookup, and so needs to occur off-thread.
 */
public class CheckUrl extends Resource<CheckUrl> {
    private static final String EVENT = "http_check";
    private final IAPIEnvironment environment;
    private final String address;
    private final String host;
    private Future<?> future;

    public CheckUrl(ResourceGroup<CheckUrl> limiter, IAPIEnvironment environment, String address, URI uri) {
        super(limiter);
        this.environment = environment;
        this.address = address;
        this.host = uri.getHost();
    }

    public void run() {
        if (this.isClosed()) {
            return;
        }
        this.future = NetworkUtils.EXECUTOR.submit(this::doRun);
        this.checkClosed();
    }

    private void doRun() {
        if (this.isClosed()) {
            return;
        }

        try {
            InetSocketAddress netAddress = NetworkUtils.getAddress(this.host, 80, false);
            NetworkUtils.getOptions(this.host, netAddress);

            if (this.tryClose()) {
                this.environment.queueEvent(EVENT, this.address, true);
            }
        } catch (HTTPRequestException e) {
            if (this.tryClose()) {
                this.environment.queueEvent(EVENT, this.address, false, e.getMessage());
            }
        }
    }

    @Override
    protected void dispose() {
        super.dispose();
        this.future = closeFuture(this.future);
    }
}
