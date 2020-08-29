/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import static dan200.computercraft.core.apis.ArgumentHelper.getInt;
import static dan200.computercraft.core.apis.ArgumentHelper.getReal;
import static dan200.computercraft.core.apis.ArgumentHelper.getString;
import static dan200.computercraft.core.apis.ArgumentHelper.optLong;
import static dan200.computercraft.core.apis.ArgumentHelper.optString;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.shared.util.StringUtil;

public class OSAPI implements ILuaAPI {
    private final Map<Integer, Timer> m_timers;
    private final Map<Integer, Alarm> m_alarms;
    private IAPIEnvironment m_apiEnvironment;
    private int m_clock;
    private double m_time;
    private int m_day;

    private int m_nextTimerToken;
    private int m_nextAlarmToken;

    public OSAPI(IAPIEnvironment environment) {
        this.m_apiEnvironment = environment;
        this.m_nextTimerToken = 0;
        this.m_nextAlarmToken = 0;
        this.m_timers = new HashMap<>();
        this.m_alarms = new HashMap<>();
    }

    @Override
    public String[] getNames() {
        return new String[] {
            "os"
        };
    }

    @Override
    public void startup() {
        this.m_time = this.m_apiEnvironment.getComputerEnvironment()
                                           .getTimeOfDay();
        this.m_day = this.m_apiEnvironment.getComputerEnvironment()
                                          .getDay();
        this.m_clock = 0;

        synchronized (this.m_timers) {
            this.m_timers.clear();
        }

        synchronized (this.m_alarms) {
            this.m_alarms.clear();
        }
    }

    // ILuaAPI implementation

    @Override
    public void update() {
        synchronized (this.m_timers) {
            // Update the clock
            this.m_clock++;

            // Countdown all of our active timers
            Iterator<Map.Entry<Integer, Timer>> it = this.m_timers.entrySet()
                                                                  .iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Timer> entry = it.next();
                Timer timer = entry.getValue();
                timer.m_ticksLeft--;
                if (timer.m_ticksLeft <= 0) {
                    // Queue the "timer" event
                    this.queueLuaEvent("timer", new Object[] {entry.getKey()});
                    it.remove();
                }
            }
        }

        // Wait for all of our alarms
        synchronized (this.m_alarms) {
            double previousTime = this.m_time;
            int previousDay = this.m_day;
            double time = this.m_apiEnvironment.getComputerEnvironment()
                                               .getTimeOfDay();
            int day = this.m_apiEnvironment.getComputerEnvironment()
                                           .getDay();

            if (time > previousTime || day > previousDay) {
                double now = this.m_day * 24.0 + this.m_time;
                Iterator<Map.Entry<Integer, Alarm>> it = this.m_alarms.entrySet()
                                                                      .iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, Alarm> entry = it.next();
                    Alarm alarm = entry.getValue();
                    double t = alarm.m_day * 24.0 + alarm.m_time;
                    if (now >= t) {
                        this.queueLuaEvent("alarm", new Object[] {entry.getKey()});
                        it.remove();
                    }
                }
            }

            this.m_time = time;
            this.m_day = day;
        }
    }

    @Override
    public void shutdown() {
        synchronized (this.m_timers) {
            this.m_timers.clear();
        }

        synchronized (this.m_alarms) {
            this.m_alarms.clear();
        }
    }

    private void queueLuaEvent(String event, Object[] args) {
        this.m_apiEnvironment.queueEvent(event, args);
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
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

    @Override
    public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException {
        switch (method) {
        case 0: // queueEvent
            this.queueLuaEvent(getString(args, 0), this.trimArray(args, 1));
            return null;
        case 1: {
            // startTimer
            double timer = getReal(args, 0);
            synchronized (this.m_timers) {
                this.m_timers.put(this.m_nextTimerToken, new Timer((int) Math.round(timer / 0.05)));
                return new Object[] {this.m_nextTimerToken++};
            }
        }
        case 2: {
            // setAlarm
            double time = getReal(args, 0);
            if (time < 0.0 || time >= 24.0) {
                throw new LuaException("Number out of range");
            }
            synchronized (this.m_alarms) {
                int day = time > this.m_time ? this.m_day : this.m_day + 1;
                this.m_alarms.put(this.m_nextAlarmToken, new Alarm(time, day));
                return new Object[] {this.m_nextAlarmToken++};
            }
        }
        case 3: // shutdown
            this.m_apiEnvironment.shutdown();
            return null;
        case 4: // reboot
            this.m_apiEnvironment.reboot();
            return null;
        case 5:
        case 6: // computerID/getComputerID
            return new Object[] {this.getComputerID()};
        case 7: {
            // setComputerLabel
            String label = optString(args, 0, null);
            this.m_apiEnvironment.setLabel(StringUtil.normaliseLabel(label));
            return null;
        }
        case 8:
        case 9: {
            // computerLabel/getComputerLabel
            String label = this.m_apiEnvironment.getLabel();
            if (label != null) {
                return new Object[] {label};
            }
            return null;
        }
        case 10: // clock
            synchronized (this.m_timers) {
                return new Object[] {this.m_clock * 0.05};
            }
        case 11: {
            // time
            Object value = args.length > 0 ? args[0] : null;
            if (value instanceof Map) {
                return new Object[] {LuaDateTime.fromTable((Map<?, ?>) value)};
            }

            String param = optString(args, 0, "ingame");
            switch (param.toLowerCase(Locale.ROOT)) {
            case "utc": {
                // Get Hour of day (UTC)
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                return new Object[] {getTimeForCalendar(c)};
            }
            case "local": {
                // Get Hour of day (local time)
                Calendar c = Calendar.getInstance();
                return new Object[] {getTimeForCalendar(c)};
            }
            case "ingame":
                // Get ingame hour
                synchronized (this.m_alarms) {
                    return new Object[] {this.m_time};
                }
            default:
                throw new LuaException("Unsupported operation");
            }
        }
        case 12: {
            // day
            String param = optString(args, 0, "ingame");
            switch (param.toLowerCase(Locale.ROOT)) {
            case "utc": {
                // Get numbers of days since 1970-01-01 (utc)
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                return new Object[] {getDayForCalendar(c)};
            }
            case "local": {
                // Get numbers of days since 1970-01-01 (local time)
                Calendar c = Calendar.getInstance();
                return new Object[] {getDayForCalendar(c)};
            }
            case "ingame":
                // Get game day
                synchronized (this.m_alarms) {
                    return new Object[] {this.m_day};
                }
            default:
                throw new LuaException("Unsupported operation");
            }
        }
        case 13: {
            // cancelTimer
            int token = getInt(args, 0);
            synchronized (this.m_timers) {
                this.m_timers.remove(token);
            }
            return null;
        }
        case 14: {
            // cancelAlarm
            int token = getInt(args, 0);
            synchronized (this.m_alarms) {
                this.m_alarms.remove(token);
            }
            return null;
        }
        case 15: // epoch
        {
            String param = optString(args, 0, "ingame");
            switch (param.toLowerCase(Locale.ROOT)) {
            case "utc": {
                // Get utc epoch
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                return new Object[] {getEpochForCalendar(c)};
            }
            case "local": {
                // Get local epoch
                Calendar c = Calendar.getInstance();
                return new Object[] {getEpochForCalendar(c)};
            }
            case "ingame":
                // Get in-game epoch
                synchronized (this.m_alarms) {
                    return new Object[] {
                        this.m_day * 86400000 + (int) (this.m_time * 3600000.0f)
                    };
                }
            default:
                throw new LuaException("Unsupported operation");
            }
        }
        case 16: // date
        {
            String format = optString(args, 0, "%c");
            long time = optLong(args,
                                1,
                                Instant.now()
                                       .getEpochSecond());

            Instant instant = Instant.ofEpochSecond(time);
            ZonedDateTime date;
            ZoneOffset offset;
            boolean isDst;
            if (format.startsWith("!")) {
                offset = ZoneOffset.UTC;
                date = ZonedDateTime.ofInstant(instant, offset);
                format = format.substring(1);
            } else {
                ZoneId id = ZoneId.systemDefault();
                offset = id.getRules()
                           .getOffset(instant);
                date = ZonedDateTime.ofInstant(instant, id);
            }

            if (format.equals("*t")) {
                return new Object[] {LuaDateTime.toTable(date, offset, instant)};
            }

            DateTimeFormatterBuilder formatter = new DateTimeFormatterBuilder();
            LuaDateTime.format(formatter, format, offset);
            return new Object[] {formatter.toFormatter(Locale.ROOT).format(date)};
        }
        default:
            return null;
        }
    }

    private Object[] trimArray(Object[] array, int skip) {
        return Arrays.copyOfRange(array, skip, array.length);
    }

    private int getComputerID() {
        return this.m_apiEnvironment.getComputerID();
    }

    private static float getTimeForCalendar(Calendar c) {
        float time = c.get(Calendar.HOUR_OF_DAY);
        time += c.get(Calendar.MINUTE) / 60.0f;
        time += c.get(Calendar.SECOND) / (60.0f * 60.0f);
        return time;
    }

    private static int getDayForCalendar(Calendar c) {
        GregorianCalendar g = c instanceof GregorianCalendar ? (GregorianCalendar) c : new GregorianCalendar();
        int year = c.get(Calendar.YEAR);
        int day = 0;
        for (int y = 1970; y < year; y++) {
            day += g.isLeapYear(y) ? 366 : 365;
        }
        day += c.get(Calendar.DAY_OF_YEAR);
        return day;
    }

    // Private methods

    private static long getEpochForCalendar(Calendar c) {
        return c.getTime()
                .getTime();
    }

    private static class Timer {
        public int m_ticksLeft;

        public Timer(int ticksLeft) {
            this.m_ticksLeft = ticksLeft;
        }
    }

    private static class Alarm implements Comparable<Alarm> {
        public final double m_time;
        public final int m_day;

        public Alarm(double time, int day) {
            this.m_time = time;
            this.m_day = day;
        }

        @Override
        public int compareTo(@Nonnull Alarm o) {
            double t = this.m_day * 24.0 + this.m_time;
            double ot = this.m_day * 24.0 + this.m_time;
            return Double.compare(t, ot);
        }
    }
}
