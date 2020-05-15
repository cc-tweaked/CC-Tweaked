/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.handles.HandleGeneric;
import dan200.computercraft.core.asm.ObjectSource;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

/**
 * Wraps a {@link dan200.computercraft.core.apis.handles.HandleGeneric} and provides additional methods for
 * getting the response code and headers.
 */
public class HttpResponseHandle implements ObjectSource
{
    private final Object reader;
    private final int responseCode;
    private final String responseStatus;
    private final Map<String, String> responseHeaders;

    public HttpResponseHandle( @Nonnull HandleGeneric reader, int responseCode, String responseStatus, @Nonnull Map<String, String> responseHeaders )
    {
        this.reader = reader;
        this.responseCode = responseCode;
        this.responseStatus = responseStatus;
        this.responseHeaders = responseHeaders;
    }

    @LuaFunction
    public final Object[] getResponseCode()
    {
        return new Object[] { responseCode, responseStatus };
    }

    @LuaFunction
    public final Map<String, String> getResponseHeaders()
    {
        return responseHeaders;
    }

    @Override
    public Iterable<Object> getExtra()
    {
        return Collections.singletonList(reader);
    }
}
