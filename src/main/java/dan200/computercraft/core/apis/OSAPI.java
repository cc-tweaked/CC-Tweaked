/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.shared.util.StringUtil;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static dan200.computercraft.api.lua.ArgumentHelper.*;

public class OSAPI implements ILuaAPI
{
    private IAPIEnvironment m_apiEnvironment;

    private final Map<Integer, Timer> m_timers;
    private final Map<Integer, Alarm> m_alarms;
    private int m_clock;
    private double m_time;
    private int m_day;

    private int m_nextTimerToken;
    private int m_nextAlarmToken;

    private static class Timer
    {
        int m_ticksLeft;

        Timer( int ticksLeft )
        {
            m_ticksLeft = ticksLeft;
        }
    }

    private static class Alarm implements Comparable<Alarm>
    {
        final double m_time;
        final int m_day;

        Alarm( double time, int day )
        {
            m_time = time;
            m_day = day;
        }

        @Override
        public int compareTo( @Nonnull Alarm o )
        {
            double t = m_day * 24.0 + m_time;
            double ot = m_day * 24.0 + m_time;
            return Double.compare( t, ot );
        }
    }

    public OSAPI( IAPIEnvironment environment )
    {
        m_apiEnvironment = environment;
        m_nextTimerToken = 0;
        m_nextAlarmToken = 0;
        m_timers = new HashMap<>();
        m_alarms = new HashMap<>();
    }

    // ILuaAPI implementation

    @Override
    public String[] getNames()
    {
        return new String[] { "os" };
    }

    @Override
    public void startup()
    {
        m_time = m_apiEnvironment.getComputerEnvironment().getTimeOfDay();
        m_day = m_apiEnvironment.getComputerEnvironment().getDay();
        m_clock = 0;

        synchronized( m_timers )
        {
            m_timers.clear();
        }

        synchronized( m_alarms )
        {
            m_alarms.clear();
        }
    }

    @Override
    public void update()
    {
        synchronized( m_timers )
        {
            // Update the clock
            m_clock++;

            // Countdown all of our active timers
            Iterator<Map.Entry<Integer, Timer>> it = m_timers.entrySet().iterator();
            while( it.hasNext() )
            {
                Map.Entry<Integer, Timer> entry = it.next();
                Timer timer = entry.getValue();
                timer.m_ticksLeft--;
                if( timer.m_ticksLeft <= 0 )
                {
                    // Queue the "timer" event
                    queueLuaEvent( "timer", new Object[] { entry.getKey() } );
                    it.remove();
                }
            }
        }

        // Wait for all of our alarms
        synchronized( m_alarms )
        {
            double previousTime = m_time;
            int previousDay = m_day;
            double time = m_apiEnvironment.getComputerEnvironment().getTimeOfDay();
            int day = m_apiEnvironment.getComputerEnvironment().getDay();

            if( time > previousTime || day > previousDay )
            {
                double now = m_day * 24.0 + m_time;
                Iterator<Map.Entry<Integer, Alarm>> it = m_alarms.entrySet().iterator();
                while( it.hasNext() )
                {
                    Map.Entry<Integer, Alarm> entry = it.next();
                    Alarm alarm = entry.getValue();
                    double t = alarm.m_day * 24.0 + alarm.m_time;
                    if( now >= t )
                    {
                        queueLuaEvent( "alarm", new Object[] { entry.getKey() } );
                        it.remove();
                    }
                }
            }

            m_time = time;
            m_day = day;
        }
    }

    @Override
    public void shutdown()
    {
        synchronized( m_timers )
        {
            m_timers.clear();
        }

        synchronized( m_alarms )
        {
            m_alarms.clear();
        }
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "queueEvent",
            "startTimer",
            "setAlarm",
            "shutdown",
            "reboot",
            "computerID",
            "getComputerID",
            "setComputerLabel",
            "computerLabel",
            "getComputerLabel",
            "clock",
            "time",
            "day",
            "cancelTimer",
            "cancelAlarm",
            "epoch",
            "date",
        };
    }

    private static float getTimeForCalendar( Calendar c )
    {
        float time = c.get( Calendar.HOUR_OF_DAY );
        time += c.get( Calendar.MINUTE ) / 60.0f;
        time += c.get( Calendar.SECOND ) / (60.0f * 60.0f);
        return time;
    }

    private static int getDayForCalendar( Calendar c )
    {
        GregorianCalendar g = c instanceof GregorianCalendar ? (GregorianCalendar) c : new GregorianCalendar();
        int year = c.get( Calendar.YEAR );
        int day = 0;
        for( int y = 1970; y < year; y++ )
        {
            day += g.isLeapYear( y ) ? 366 : 365;
        }
        day += c.get( Calendar.DAY_OF_YEAR );
        return day;
    }

    private static long getEpochForCalendar( Calendar c )
    {
        return c.getTime().getTime();
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0: // queueEvent
                queueLuaEvent( getString( args, 0 ), trimArray( args, 1 ) );
                return null;
            case 1:
            {
                // startTimer
                double timer = getFiniteDouble( args, 0 );
                synchronized( m_timers )
                {
                    m_timers.put( m_nextTimerToken, new Timer( (int) Math.round( timer / 0.05 ) ) );
                    return new Object[] { m_nextTimerToken++ };
                }
            }
            case 2:
            {
                // setAlarm
                double time = getFiniteDouble( args, 0 );
                if( time < 0.0 || time >= 24.0 )
                {
                    throw new LuaException( "Number out of range" );
                }
                synchronized( m_alarms )
                {
                    int day = time > m_time ? m_day : m_day + 1;
                    m_alarms.put( m_nextAlarmToken, new Alarm( time, day ) );
                    return new Object[] { m_nextAlarmToken++ };
                }
            }
            case 3: // shutdown
                m_apiEnvironment.shutdown();
                return null;
            case 4: // reboot
                m_apiEnvironment.reboot();
                return null;
            case 5:
            case 6: // computerID/getComputerID
                return new Object[] { getComputerID() };
            case 7:
            {
                // setComputerLabel
                String label = optString( args, 0, null );
                m_apiEnvironment.setLabel( StringUtil.normaliseLabel( label ) );
                return null;
            }
            case 8:
            case 9:
            {
                // computerLabel/getComputerLabel
                String label = m_apiEnvironment.getLabel();
                if( label != null )
                {
                    return new Object[] { label };
                }
                return null;
            }
            case 10: // clock
                synchronized( m_timers )
                {
                    return new Object[] { m_clock * 0.05 };
                }
            case 11:
            {
                // time
                Object value = args.length > 0 ? args[0] : null;
                if( value instanceof Map ) return new Object[] { LuaDateTime.fromTable( (Map<?, ?>) value ) };

                String param = optString( args, 0, "ingame" );
                switch( param.toLowerCase( Locale.ROOT ) )
                {
                    case "utc":
                    {
                        // Get Hour of day (UTC)
                        Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
                        return new Object[] { getTimeForCalendar( c ) };
                    }
                    case "local":
                    {
                        // Get Hour of day (local time)
                        Calendar c = Calendar.getInstance();
                        return new Object[] { getTimeForCalendar( c ) };
                    }
                    case "ingame":
                        // Get ingame hour
                        synchronized( m_alarms )
                        {
                            return new Object[] { m_time };
                        }
                    default:
                        throw new LuaException( "Unsupported operation" );
                }
            }
            case 12:
            {
                // day
                String param = optString( args, 0, "ingame" );
                switch( param.toLowerCase( Locale.ROOT ) )
                {
                    case "utc":
                    {
                        // Get numbers of days since 1970-01-01 (utc)
                        Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
                        return new Object[] { getDayForCalendar( c ) };
                    }
                    case "local":
                    {
                        // Get numbers of days since 1970-01-01 (local time)
                        Calendar c = Calendar.getInstance();
                        return new Object[] { getDayForCalendar( c ) };
                    }
                    case "ingame":
                        // Get game day
                        synchronized( m_alarms )
                        {
                            return new Object[] { m_day };
                        }
                    default:
                        throw new LuaException( "Unsupported operation" );
                }
            }
            case 13:
            {
                // cancelTimer
                int token = getInt( args, 0 );
                synchronized( m_timers )
                {
                    m_timers.remove( token );
                }
                return null;
            }
            case 14:
            {
                // cancelAlarm
                int token = getInt( args, 0 );
                synchronized( m_alarms )
                {
                    m_alarms.remove( token );
                }
                return null;
            }
            case 15: // epoch
            {
                String param = optString( args, 0, "ingame" );
                switch( param.toLowerCase( Locale.ROOT ) )
                {
                    case "utc":
                    {
                        // Get utc epoch
                        Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
                        return new Object[] { getEpochForCalendar( c ) };
                    }
                    case "local":
                    {
                        // Get local epoch
                        Calendar c = Calendar.getInstance();
                        return new Object[] { getEpochForCalendar( c ) };
                    }
                    case "ingame":
                        // Get in-game epoch
                        synchronized( m_alarms )
                        {
                            return new Object[] { m_day * 86400000 + (int) (m_time * 3600000.0f) };
                        }
                    default:
                        throw new LuaException( "Unsupported operation" );
                }
            }
            case 16: // date
            {
                String format = optString( args, 0, "%c" );
                long time = optLong( args, 1, Instant.now().getEpochSecond() );

                Instant instant = Instant.ofEpochSecond( time );
                ZonedDateTime date;
                ZoneOffset offset;
                if( format.startsWith( "!" ) )
                {
                    offset = ZoneOffset.UTC;
                    date = ZonedDateTime.ofInstant( instant, offset );
                    format = format.substring( 1 );
                }
                else
                {
                    ZoneId id = ZoneId.systemDefault();
                    offset = id.getRules().getOffset( instant );
                    date = ZonedDateTime.ofInstant( instant, id );
                }

                if( format.equals( "*t" ) ) return new Object[] { LuaDateTime.toTable( date, offset, instant ) };

                DateTimeFormatterBuilder formatter = new DateTimeFormatterBuilder();
                LuaDateTime.format( formatter, format, offset );
                return new Object[] { formatter.toFormatter( Locale.ROOT ).format( date ) };
            }
            default:
                return null;
        }
    }

    // Private methods

    private void queueLuaEvent( String event, Object[] args )
    {
        m_apiEnvironment.queueEvent( event, args );
    }

    private Object[] trimArray( Object[] array, int skip )
    {
        return Arrays.copyOfRange( array, skip, array.length );
    }

    private int getComputerID()
    {
        return m_apiEnvironment.getComputerID();
    }
}
