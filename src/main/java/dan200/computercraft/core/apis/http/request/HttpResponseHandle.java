/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;

/**
 * Wraps a {@link dan200.computercraft.core.apis.handles.HandleGeneric} and provides additional methods for
 * getting the response code and headers.
 */
public class HttpResponseHandle implements ILuaObject
{
    private final String[] newMethods;
    private final int methodOffset;
    private final ILuaObject reader;
    private final int responseCode;
    private final String responseStatus;
    private final Map<String, String> responseHeaders;

    public HttpResponseHandle( @Nonnull ILuaObject reader, int responseCode, String responseStatus, @Nonnull Map<String, String> responseHeaders )
    {
        this.reader = reader;
        this.responseCode = responseCode;
        this.responseStatus = responseStatus;
        this.responseHeaders = responseHeaders;

        String[] oldMethods = reader.getMethodNames();
        final int methodOffset = this.methodOffset = oldMethods.length;

        final String[] newMethods = this.newMethods = Arrays.copyOf( oldMethods, oldMethods.length + 2 );
        newMethods[methodOffset + 0] = "getResponseCode";
        newMethods[methodOffset + 1] = "getResponseHeaders";
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return newMethods;
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException, InterruptedException
    {
        if( method < methodOffset ) return reader.callMethod( context, method, args );

        switch( method - methodOffset )
        {
            case 0: // getResponseCode
                return new Object[] { responseCode, responseStatus };
            case 1: // getResponseHeaders
                return new Object[] { responseHeaders };
            default:
                return null;
        }
    }
}
