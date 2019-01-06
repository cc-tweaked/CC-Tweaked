/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import dan200.computercraft.core.apis.HTTPAPI;
import dan200.computercraft.core.apis.IAPIEnvironment;

import java.net.URI;
import java.util.concurrent.Future;

/**
 * Checks a URL using {@link NetworkUtils#getAddress(String, int, boolean)}}
 *
 * This requires a DNS lookup, and so needs to occur off-thread.
 */
public class CheckUrl extends MonitorerdResource
{
    private static final String EVENT = "http_check";

    private Future<?> future;

    private final IAPIEnvironment environment;
    private final HTTPAPI api;
    private final String address;
    private final String host;

    public CheckUrl( IAPIEnvironment environment, HTTPAPI api, String address, URI uri )
    {
        this.environment = environment;
        this.api = api;
        this.address = address;
        this.host = uri.getHost();
    }

    public void run()
    {
        if( isClosed() ) return;
        future = NetworkUtils.EXECUTOR.submit( this::doRun );
    }

    private void doRun()
    {
        if( isClosed() ) return;

        try
        {
            NetworkUtils.getAddress( host, 80, false );
            if( tryClose() ) environment.queueEvent( EVENT, new Object[] { address, true } );
        }
        catch( HTTPRequestException e )
        {
            if( tryClose() ) environment.queueEvent( EVENT, new Object[] { address, false, e.getMessage() } );
        }
    }

    @Override
    protected void dispose()
    {
        api.removeCloseable( this );

        future = closeFuture( future );
    }
}
