// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.util;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;

public enum Holiday {
    NONE,

    /**
     * 14th February.
     */
    VALENTINES,

    /**
     * 24th-26th December.
     *
     * @see net.minecraft.client.renderer.blockentity.ChestRenderer
     */
    CHRISTMAS;

    public static Holiday getCurrent() {
        var now = LocalDateTime.now(ZoneId.systemDefault());
        var month = now.getMonth();
        var day = now.getDayOfMonth();
        if (month == Month.FEBRUARY && day == 14) return VALENTINES;
        if (month == Month.DECEMBER && day >= 24 && day <= 26) return CHRISTMAS;
        return NONE;
    }
}
