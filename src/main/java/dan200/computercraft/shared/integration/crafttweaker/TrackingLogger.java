/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker;

import com.blamejared.crafttweaker.api.logger.ILogger;

/**
 * Logger which tracks if it has any messages.
 */
public final class TrackingLogger
{
    private final ILogger logger;
    private boolean ok = true;

    public TrackingLogger( ILogger logger )
    {
        this.logger = logger;
    }

    public boolean isOk()
    {
        return ok;
    }

    public void warning( String message )
    {
        logger.warning( message );
    }

    public void error( String message )
    {
        ok = false;
        logger.error( message );
    }
}
