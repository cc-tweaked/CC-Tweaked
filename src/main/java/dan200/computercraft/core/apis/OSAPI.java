/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.shared.util.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static dan200.computercraft.core.apis.ArgumentHelper.*;

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
        public int m_ticksLeft;

        public Timer( int ticksLeft )
        {
            m_ticksLeft = ticksLeft;
        }
    }
    
    private class Alarm implements Comparable<Alarm>
    {
        public final double m_time;
        public final int m_day;

        public Alarm( double time, int day )
        {
            m_time = time;
            m_day = day;
        }

        @Override
        public int compareTo( @Nonnull Alarm o )
        {
            double t = m_day * 24.0 + m_time;
            double ot = m_day * 24.0 + m_time;
            if( t < ot ) {
                return -1;
            } else if( t > ot ) {
                return 1;
            } else {
                return 0;
            }
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
        return new String[] {
            "os"
        };
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
                timer.m_ticksLeft = timer.m_ticksLeft - 1;
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
            int day =  m_apiEnvironment.getComputerEnvironment().getDay();
            
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
                        queueLuaEvent( "alarm", new Object[]{ entry.getKey() } );
                        it.remove();
                    }
                }
            }

            m_time = time;
            m_day = day;
        }
    }
    
    @Override
    public void shutdown( )
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
            "epoch"
        };
    }

    private float getTimeForCalendar(Calendar c)
    {
        float time = c.get(Calendar.HOUR_OF_DAY);
        time += c.get(Calendar.MINUTE) / 60.0f;
        time += c.get(Calendar.SECOND) / (60.0f * 60.0f);
        return time;
    }

    private int getDayForCalendar(Calendar c)
    {
        GregorianCalendar g = (c instanceof GregorianCalendar) ? (GregorianCalendar)c : new GregorianCalendar();
        int year = c.get(Calendar.YEAR);
        int day = 0;
        for( int y=1970; y<year; ++y )
        {
            day += g.isLeapYear(y) ? 366 : 365;
        }
        day += c.get(Calendar.DAY_OF_YEAR);
        return day;
    }

    private long getEpochForCalendar(Calendar c)
    {
        return c.getTime().getTime();
    }

    @Override
    @Nonnull
    public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                // queueEvent
                queueLuaEvent( getString( args, 0 ), trimArray( args, 1 ) );
                return MethodResult.empty();
            }
            case 1:
            {
                // startTimer
                double timer = getReal( args, 0 );
                synchronized( m_timers )
                {
                    m_timers.put( m_nextTimerToken, new Timer( (int)Math.round( timer / 0.05 ) ) );
                    return MethodResult.of( m_nextTimerToken++ );
                }
            }
            case 2:
            {
                // setAlarm
                double time = getReal( args, 0 );
                if( time < 0.0 || time >= 24.0 )
                {
                    throw new LuaException( "Number out of range" );
                }                
                synchronized( m_alarms )
                {
                    int day = (time > m_time) ? m_day : (m_day + 1);
                    m_alarms.put( m_nextAlarmToken, new Alarm( time, day ) );
                    return MethodResult.of( m_nextAlarmToken++ );
                }
            }
            case 3:
            {
                // shutdown
                m_apiEnvironment.shutdown();
                return MethodResult.empty();
            }
            case 4:
            {
                // reboot
                m_apiEnvironment.reboot();
                return MethodResult.empty();
            }
            case 5:
            case 6:
            {
                // computerID/getComputerID
                return MethodResult.of( getComputerID() );
            }
            case 7:
            {
                // setComputerLabel
                String label = optString( args, 0, null );
                m_apiEnvironment.setLabel( StringUtil.normaliseLabel( label ) );
                return MethodResult.empty();
            }
            case 8:
            case 9:
            {
                // computerLabel/getComputerLabel
                String label = m_apiEnvironment.getLabel();
                if( label != null )
                {
                    return MethodResult.of( label );
                }
                return MethodResult.empty();
            }
            case 10:
            {
                // clock
                synchronized( m_timers )
                {
                    return MethodResult.of( m_clock * 0.05 );
                }
            }
            case 11:
            {
                // time
                String param = optString( args, 0, "ingame" );
                switch( param )
                {
                    case "utc":
                    {
                        // Get Hour of day (UTC)
                        Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
                        return MethodResult.of( getTimeForCalendar( c ) );
                    }
                    case "local":
                    {
                        // Get Hour of day (local time)
                        Calendar c = Calendar.getInstance();
                        return MethodResult.of( getTimeForCalendar( c ) );
                    }
                    case "ingame":
                        // Get ingame hour
                        synchronized( m_alarms )
                        {
                            return MethodResult.of( m_time );
                        }
                    default:
                        throw new LuaException( "Unsupported operation" );
                }
            }
            case 12:
            {
                // day
                String param = optString( args, 0, "ingame" );
                switch( param )
                {
                    case "utc":
                    {
                        // Get numbers of days since 1970-01-01 (utc)
                        Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
                        return MethodResult.of( getDayForCalendar( c ) );
                    }
                    case "local":
                    {
                        // Get numbers of days since 1970-01-01 (local time)
                        Calendar c = Calendar.getInstance();
                        return MethodResult.of( getDayForCalendar( c ) );
                    }
                    case "ingame":
                        // Get game day
                        synchronized( m_alarms )
                        {
                            return MethodResult.of( m_day );
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
                    if( m_timers.containsKey( token ) )
                    {
                        m_timers.remove( token );
                    }
                }
                return MethodResult.empty();
            }
            case 14:
            {
                // cancelAlarm
                int token = getInt( args, 0 );
                synchronized( m_alarms )
                {
                    if( m_alarms.containsKey( token ) )
                    {
                        m_alarms.remove( token );
                    }
                }
                return MethodResult.empty();
            }
            case 15:
            {
                // epoch
                String param = optString( args, 0, "ingame" );
                switch( param )
                {
                    case "utc":
                    {
                        // Get utc epoch
                        Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
                        return MethodResult.of( getEpochForCalendar( c ) );
                    }
                    case "local":
                    {
                        // Get local epoch
                        Calendar c = Calendar.getInstance();
                        return MethodResult.of( getEpochForCalendar( c ) );
                    }
                    case "ingame":
                        // Get in-game epoch
                        synchronized( m_alarms )
                        {
                            return MethodResult.of(
                                m_day * 86400000 + (int) (m_time * 3600000.0f)
                            );
                        }
                    default:
                        throw new LuaException( "Unsupported operation" );
                }
            }
            default:
            {
                return MethodResult.empty();
            }
        }
    }

    @Nullable
    @Override
    @Deprecated
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        return callMethod( (ICallContext) context, method, arguments ).evaluate( context );
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
