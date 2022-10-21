/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

public interface IComputerEnvironment
{
    int getDay();

    double getTimeOfDay();

    @Nonnull
    String getHostString();

    @Nonnull
    String getUserAgent();

    @Nullable
    IWritableMount createRootMount();

    @Nullable
    IMount createResourceMount( String domain, String subPath );

    @Nullable
    InputStream createResourceFile( String domain, String subPath );
}
