/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import java.io.Closeable;
import java.io.IOException;

public final class IoUtil
{
    private IoUtil() {}

    public static void closeQuietly( Closeable closeable )
    {
        try
        {
            closeable.close();
        }
        catch( IOException ignored )
        {
        }
    }
}
