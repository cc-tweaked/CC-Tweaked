/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.options;

import javax.annotation.Nonnull;
import java.util.OptionalInt;
import java.util.OptionalLong;

public enum Action
{
    ALLOW,
    DENY;

    private final PartialOptions partial = new PartialOptions(
        this, OptionalLong.empty(), OptionalLong.empty(), OptionalInt.empty(), OptionalInt.empty()
    );

    @Nonnull
    public PartialOptions toPartial()
    {
        return partial;
    }
}
