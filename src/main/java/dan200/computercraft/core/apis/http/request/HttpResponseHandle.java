/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.HTTPAPI;
import dan200.computercraft.core.apis.handles.BinaryReadableHandle;
import dan200.computercraft.core.apis.handles.EncodedReadableHandle;
import dan200.computercraft.core.apis.handles.HandleGeneric;
import dan200.computercraft.core.asm.ObjectSource;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

/**
 * A http response. This provides the same methods as a {@link EncodedReadableHandle file} (or
 * {@link BinaryReadableHandle binary file} if the request used binary mode), though provides several request specific
 * methods.
 *
 * @cc.module http.Response
 * @see HTTPAPI#request(IArguments)  On how to make a http request.
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

    /**
     * Returns the response code and response message returned by the server.
     *
     * @return The response code and message.
     * @cc.treturn number The response code (i.e. 200)
     * @cc.treturn string The response message (i.e. "OK")
     * @cc.changed 1.80pr1.13 Added response message return value.
     */
    @LuaFunction
    public final Object[] getResponseCode()
    {
        return new Object[] { responseCode, responseStatus };
    }

    /**
     * Get a table containing the response's headers, in a format similar to that required by {@link HTTPAPI#request}.
     * If multiple headers are sent with the same name, they will be combined with a comma.
     *
     * @return The response's headers.
     * @cc.usage Make a request to [example.tweaked.cc](https://example.tweaked.cc), and print the
     * returned headers.
     * <pre>{@code
     * local request = http.get("https://example.tweaked.cc")
     * print(textutils.serialize(request.getResponseHeaders()))
     * -- => {
     * --  [ "Content-Type" ] = "text/plain; charset=utf8",
     * --  [ "content-length" ] = 17,
     * --  ...
     * -- }
     * request.close()
     * }</pre>
     */
    @LuaFunction
    public final Map<String, String> getResponseHeaders()
    {
        return responseHeaders;
    }

    @Override
    public Iterable<Object> getExtra()
    {
        return Collections.singletonList( reader );
    }
}
