/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.LuaException;

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

final class LuaDateTime
{
    private LuaDateTime()
    {
    }

    static void format( DateTimeFormatterBuilder formatter, String format, ZoneOffset offset ) throws LuaException
    {
        for( int i = 0; i < format.length(); )
        {
            char c;
            switch( c = format.charAt( i++ ) )
            {
                case '\n':
                    formatter.appendLiteral( '\n' );
                    break;
                default:
                    formatter.appendLiteral( c );
                    break;
                case '%':
                    if( i >= format.length() ) break;
                    switch( c = format.charAt( i++ ) )
                    {
                        default:
                            throw new LuaException( "bad argument #1: invalid conversion specifier '%" + c + "'" );

                        case '%':
                            formatter.appendLiteral( '%' );
                            break;
                        case 'a':
                            formatter.appendText( ChronoField.DAY_OF_WEEK, TextStyle.SHORT );
                            break;
                        case 'A':
                            formatter.appendText( ChronoField.DAY_OF_WEEK, TextStyle.FULL );
                            break;
                        case 'b':
                        case 'h':
                            formatter.appendText( ChronoField.MONTH_OF_YEAR, TextStyle.SHORT );
                            break;
                        case 'B':
                            formatter.appendText( ChronoField.MONTH_OF_YEAR, TextStyle.FULL );
                            break;
                        case 'c':
                            format( formatter, "%a %b %e %H:%M:%S %Y", offset );
                            break;
                        case 'C':
                            formatter.appendValueReduced( CENTURY, 2, 2, 0 );
                            break;
                        case 'd':
                            formatter.appendValue( ChronoField.DAY_OF_MONTH, 2 );
                            break;
                        case 'D':
                        case 'x':
                            format( formatter, "%m/%d/%y", offset );
                            break;
                        case 'e':
                            formatter.padNext( 2 ).appendValue( ChronoField.DAY_OF_MONTH );
                            break;
                        case 'F':
                            format( formatter, "%Y-%m-%d", offset );
                            break;
                        case 'g':
                            formatter.appendValueReduced( IsoFields.WEEK_BASED_YEAR, 2, 2, 0 );
                            break;
                        case 'G':
                            formatter.appendValue( IsoFields.WEEK_BASED_YEAR );
                            break;
                        case 'H':
                            formatter.appendValue( ChronoField.HOUR_OF_DAY, 2 );
                            break;
                        case 'I':
                            formatter.appendValue( ChronoField.HOUR_OF_AMPM, 2 );
                            break;
                        case 'j':
                            formatter.appendValue( ChronoField.DAY_OF_YEAR, 3 );
                            break;
                        case 'm':
                            formatter.appendValue( ChronoField.MONTH_OF_YEAR, 2 );
                            break;
                        case 'M':
                            formatter.appendValue( ChronoField.MINUTE_OF_HOUR, 2 );
                            break;
                        case 'n':
                            formatter.appendLiteral( '\n' );
                            break;
                        case 'p':
                            formatter.appendText( ChronoField.AMPM_OF_DAY );
                            break;
                        case 'r':
                            format( formatter, "%I:%M:%S %p", offset );
                            break;
                        case 'R':
                            format( formatter, "%H:%M", offset );
                            break;
                        case 'S':
                            formatter.appendValue( ChronoField.SECOND_OF_MINUTE, 2 );
                            break;
                        case 't':
                            formatter.appendLiteral( '\t' );
                            break;
                        case 'T':
                        case 'X':
                            format( formatter, "%H:%M:%S", offset );
                            break;
                        case 'u':
                            formatter.appendValue( ChronoField.DAY_OF_WEEK );
                            break;
                        case 'U':
                            formatter.appendValue( ChronoField.ALIGNED_WEEK_OF_YEAR, 2 );
                            break;
                        case 'V':
                            formatter.appendValue( IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2 );
                            break;
                        case 'w':
                            formatter.appendValue( ZERO_WEEK );
                            break;
                        case 'W':
                            formatter.appendValue( WeekFields.ISO.weekOfYear(), 2 );
                            break;
                        case 'y':
                            formatter.appendValueReduced( ChronoField.YEAR, 2, 2, 0 );
                            break;
                        case 'Y':
                            formatter.appendValue( ChronoField.YEAR );
                            break;
                        case 'z':
                            formatter.appendOffset( "+HHMM", "+0000" );
                            break;
                        case 'Z':
                            formatter.appendChronologyId();
                            break;
                    }
            }
        }
    }

    static long fromTable( Map<?, ?> table ) throws LuaException
    {
        int year = getField( table, "year", -1 );
        int month = getField( table, "month", -1 );
        int day = getField( table, "day", -1 );
        int hour = getField( table, "hour", 12 );
        int minute = getField( table, "min", 12 );
        int second = getField( table, "sec", 12 );
        LocalDateTime time = LocalDateTime.of( year, month, day, hour, minute, second );

        Boolean isDst = getBoolField( table, "isdst" );
        if( isDst != null )
        {
            boolean requireDst = isDst;
            for( ZoneOffset possibleOffset : ZoneOffset.systemDefault().getRules().getValidOffsets( time ) )
            {
                Instant instant = time.toInstant( possibleOffset );
                if( possibleOffset.getRules().getDaylightSavings( instant ).isZero() == requireDst )
                {
                    return instant.getEpochSecond();
                }
            }
        }

        ZoneOffset offset = ZoneOffset.systemDefault().getRules().getOffset( time );
        return time.toInstant( offset ).getEpochSecond();
    }

    static Map<String, ?> toTable( TemporalAccessor date, ZoneId offset, Instant instant )
    {
        HashMap<String, Object> table = new HashMap<>( 9 );
        table.put( "year", date.getLong( ChronoField.YEAR ) );
        table.put( "month", date.getLong( ChronoField.MONTH_OF_YEAR ) );
        table.put( "day", date.getLong( ChronoField.DAY_OF_MONTH ) );
        table.put( "hour", date.getLong( ChronoField.HOUR_OF_DAY ) );
        table.put( "min", date.getLong( ChronoField.MINUTE_OF_HOUR ) );
        table.put( "sec", date.getLong( ChronoField.SECOND_OF_MINUTE ) );
        table.put( "wday", date.getLong( WeekFields.SUNDAY_START.dayOfWeek() ) );
        table.put( "yday", date.getLong( ChronoField.DAY_OF_YEAR ) );
        table.put( "isdst", offset.getRules().isDaylightSavings( instant ) );
        return table;
    }

    private static int getField( Map<?, ?> table, String field, int def ) throws LuaException
    {
        Object value = table.get( field );
        if( value instanceof Number ) return ((Number) value).intValue();
        if( def < 0 ) throw new LuaException( "field \"" + field + "\" missing in date table" );
        return def;
    }

    private static Boolean getBoolField( Map<?, ?> table, String field ) throws LuaException
    {
        Object value = table.get( field );
        if( value instanceof Boolean || value == null ) return (Boolean) value;
        throw new LuaException( "field \"" + field + "\" missing in date table" );
    }

    private static final TemporalField CENTURY = map( ChronoField.YEAR, ValueRange.of( 0, 6 ), x -> (x / 100) % 100 );
    private static final TemporalField ZERO_WEEK = map( WeekFields.SUNDAY_START.dayOfWeek(), ValueRange.of( 0, 6 ), x -> x - 1 );

    private static TemporalField map( TemporalField field, ValueRange range, LongUnaryOperator convert )
    {
        return new TemporalField()
        {
            private final ValueRange range = ValueRange.of( 0, 99 );

            @Override
            public TemporalUnit getBaseUnit()
            {
                return field.getBaseUnit();
            }

            @Override
            public TemporalUnit getRangeUnit()
            {
                return field.getRangeUnit();
            }

            @Override
            public ValueRange range()
            {
                return range;
            }

            @Override
            public boolean isDateBased()
            {
                return field.isDateBased();
            }

            @Override
            public boolean isTimeBased()
            {
                return field.isTimeBased();
            }

            @Override
            public boolean isSupportedBy( TemporalAccessor temporal )
            {
                return field.isSupportedBy( temporal );
            }

            @Override
            public ValueRange rangeRefinedBy( TemporalAccessor temporal )
            {
                return range;
            }

            @Override
            public long getFrom( TemporalAccessor temporal )
            {
                return convert.applyAsLong( temporal.getLong( field ) );
            }

            @Override
            @SuppressWarnings( "unchecked" )
            public <R extends Temporal> R adjustInto( R temporal, long newValue )
            {
                return (R) temporal.with( field, newValue );
            }
        };
    }
}
