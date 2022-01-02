/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import java.util.Calendar;

public final class HolidayUtil
{
    private HolidayUtil() {}

    public static Holiday getCurrentHoliday()
    {
        return getHoliday( Calendar.getInstance() );
    }

    private static Holiday getHoliday( Calendar calendar )
    {
        int month = calendar.get( Calendar.MONTH );
        int day = calendar.get( Calendar.DAY_OF_MONTH );
        if( month == Calendar.FEBRUARY && day == 14 ) return Holiday.VALENTINES;
        if( month == Calendar.APRIL && day == 1 ) return Holiday.APRIL_FOOLS_DAY;
        if( month == Calendar.OCTOBER && day == 31 ) return Holiday.HALLOWEEN;
        if( month == Calendar.DECEMBER && day >= 24 && day <= 30 ) return Holiday.CHRISTMAS;
        return Holiday.NONE;
    }
}
