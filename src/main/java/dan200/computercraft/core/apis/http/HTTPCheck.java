/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import dan200.computercraft.core.apis.IAPIEnvironment;

import java.net.URL;

public class HTTPCheck implements Runnable
{
    private final IAPIEnvironment environment;
    private final String urlString;
    private final URL url;

    public HTTPCheck( IAPIEnvironment environment, String urlString, URL url )
    {
        this.environment = environment;
        this.urlString = urlString;
        this.url = url;
    }

    @Override
    public void run()
    {
        try
        {
            HTTPRequest.checkHost( url.getHost() );
            environment.queueEvent( "http_check", new Object[] { urlString, true } );
        }
        catch( HTTPRequestException e )
        {
            environment.queueEvent( "http_check", new Object[] { urlString, false, e.getMessage() } );
        }
    }
}
