/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http;

public class HTTPRequestException extends Exception
{
    private static final long serialVersionUID = 7591208619422744652L;

    public HTTPRequestException( String s )
    {
        super( s );
    }

    @Override
    public Throwable fillInStackTrace()
    {
        return this;
    }
}
