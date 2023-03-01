// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongUnaryOperator;

final class LuaDateTime {
    private LuaDateTime() {
    }

    static void format(DateTimeFormatterBuilder formatter, String format) throws LuaException {
        for (var i = 0; i < format.length(); ) {
            char c;
            switch (c = format.charAt(i++)) {
                case '\n' -> formatter.appendLiteral('\n');
                default -> formatter.appendLiteral(c);
                case '%' -> {
                    if (i >= format.length()) break;
                    switch (c = format.charAt(i++)) {
                        default -> throw new LuaException("bad argument #1: invalid conversion specifier '%" + c + "'");
                        case '%' -> formatter.appendLiteral('%');
                        case 'a' -> formatter.appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT);
                        case 'A' -> formatter.appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL);
                        case 'b', 'h' -> formatter.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT);
                        case 'B' -> formatter.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL);
                        case 'c' -> format(formatter, "%a %b %e %H:%M:%S %Y");
                        case 'C' -> formatter.appendValueReduced(CENTURY, 2, 2, 0);
                        case 'd' -> formatter.appendValue(ChronoField.DAY_OF_MONTH, 2);
                        case 'D', 'x' -> format(formatter, "%m/%d/%y");
                        case 'e' -> formatter.padNext(2).appendValue(ChronoField.DAY_OF_MONTH);
                        case 'F' -> format(formatter, "%Y-%m-%d");
                        case 'g' -> formatter.appendValueReduced(IsoFields.WEEK_BASED_YEAR, 2, 2, 0);
                        case 'G' -> formatter.appendValue(IsoFields.WEEK_BASED_YEAR);
                        case 'H' -> formatter.appendValue(ChronoField.HOUR_OF_DAY, 2);
                        case 'I' -> formatter.appendValue(ChronoField.CLOCK_HOUR_OF_AMPM, 2);
                        case 'j' -> formatter.appendValue(ChronoField.DAY_OF_YEAR, 3);
                        case 'm' -> formatter.appendValue(ChronoField.MONTH_OF_YEAR, 2);
                        case 'M' -> formatter.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
                        case 'n' -> formatter.appendLiteral('\n');
                        case 'p' -> formatter.appendText(ChronoField.AMPM_OF_DAY);
                        case 'r' -> format(formatter, "%I:%M:%S %p");
                        case 'R' -> format(formatter, "%H:%M");
                        case 'S' -> formatter.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
                        case 't' -> formatter.appendLiteral('\t');
                        case 'T', 'X' -> format(formatter, "%H:%M:%S");
                        case 'u' -> formatter.appendValue(ChronoField.DAY_OF_WEEK);
                        case 'U' -> formatter.appendValue(ChronoField.ALIGNED_WEEK_OF_YEAR, 2);
                        case 'V' -> formatter.appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2);
                        case 'w' -> formatter.appendValue(ZERO_WEEK);
                        case 'W' -> formatter.appendValue(WeekFields.ISO.weekOfYear(), 2);
                        case 'y' -> formatter.appendValueReduced(ChronoField.YEAR, 2, 2, 0);
                        case 'Y' -> formatter.appendValue(ChronoField.YEAR);
                        case 'z' -> formatter.appendOffset("+HHMM", "+0000");
                        case 'Z' -> formatter.appendChronologyId();
                    }
                }
            }
        }
    }

    static long fromTable(Map<?, ?> table) throws LuaException {
        var year = getField(table, "year", -1);
        var month = getField(table, "month", -1);
        var day = getField(table, "day", -1);
        var hour = getField(table, "hour", 12);
        var minute = getField(table, "min", 12);
        var second = getField(table, "sec", 12);
        var time = LocalDateTime.of(year, month, day, hour, minute, second);

        var isDst = getBoolField(table, "isdst");
        if (isDst != null) {
            boolean requireDst = isDst;
            for (var possibleOffset : ZoneOffset.systemDefault().getRules().getValidOffsets(time)) {
                var instant = time.toInstant(possibleOffset);
                if (possibleOffset.getRules().getDaylightSavings(instant).isZero() == requireDst) {
                    return instant.getEpochSecond();
                }
            }
        }

        var offset = ZoneOffset.systemDefault().getRules().getOffset(time);
        return time.toInstant(offset).getEpochSecond();
    }

    static Map<String, ?> toTable(TemporalAccessor date, ZoneId offset, Instant instant) {
        var table = new HashMap<String, Object>(9);
        table.put("year", date.getLong(ChronoField.YEAR));
        table.put("month", date.getLong(ChronoField.MONTH_OF_YEAR));
        table.put("day", date.getLong(ChronoField.DAY_OF_MONTH));
        table.put("hour", date.getLong(ChronoField.HOUR_OF_DAY));
        table.put("min", date.getLong(ChronoField.MINUTE_OF_HOUR));
        table.put("sec", date.getLong(ChronoField.SECOND_OF_MINUTE));
        table.put("wday", date.getLong(WeekFields.SUNDAY_START.dayOfWeek()));
        table.put("yday", date.getLong(ChronoField.DAY_OF_YEAR));
        table.put("isdst", offset.getRules().isDaylightSavings(instant));
        return table;
    }

    private static int getField(Map<?, ?> table, String field, int def) throws LuaException {
        var value = table.get(field);
        if (value instanceof Number) return ((Number) value).intValue();
        if (def < 0) throw new LuaException("field \"" + field + "\" missing in date table");
        return def;
    }

    @Nullable
    private static Boolean getBoolField(Map<?, ?> table, String field) throws LuaException {
        var value = table.get(field);
        if (value instanceof Boolean || value == null) return (Boolean) value;
        throw new LuaException("field \"" + field + "\" missing in date table");
    }

    private static final TemporalField CENTURY = map(ChronoField.YEAR, ValueRange.of(0, 99), x -> (x / 100) % 100);
    private static final TemporalField ZERO_WEEK = map(WeekFields.SUNDAY_START.dayOfWeek(), ValueRange.of(0, 6), x -> x - 1);

    private static TemporalField map(TemporalField field, ValueRange range, LongUnaryOperator convert) {
        return new TemporalField() {
            @Override
            public TemporalUnit getBaseUnit() {
                return field.getBaseUnit();
            }

            @Override
            public TemporalUnit getRangeUnit() {
                return field.getRangeUnit();
            }

            @Override
            public ValueRange range() {
                return range;
            }

            @Override
            public boolean isDateBased() {
                return field.isDateBased();
            }

            @Override
            public boolean isTimeBased() {
                return field.isTimeBased();
            }

            @Override
            public boolean isSupportedBy(TemporalAccessor temporal) {
                return field.isSupportedBy(temporal);
            }

            @Override
            public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
                return range;
            }

            @Override
            public long getFrom(TemporalAccessor temporal) {
                return convert.applyAsLong(temporal.getLong(field));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R extends Temporal> R adjustInto(R temporal, long newValue) {
                return (R) temporal.with(field, newValue);
            }
        };
    }
}
