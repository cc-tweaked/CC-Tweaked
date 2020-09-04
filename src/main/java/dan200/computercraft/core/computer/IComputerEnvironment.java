/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;

public interface IComputerEnvironment {
    int getDay();

    double getTimeOfDay();

    boolean isColour();

    long getComputerSpaceLimit();

    @Nonnull
    String getHostString();

    @Nonnull
    String getUserAgent();

    int assignNewID();

    @Nullable
    IWritableMount createSaveDirMount(String subPath, long capacity);

    @Nullable
    IMount createResourceMount(String domain, String subPath);

    @Nullable
    InputStream createResourceFile(String domain, String subPath);
}
