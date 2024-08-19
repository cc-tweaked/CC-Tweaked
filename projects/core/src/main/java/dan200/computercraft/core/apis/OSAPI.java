// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.util.StringUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static dan200.computercraft.api.lua.LuaValues.checkFinite;

/**
 * The {@link OSAPI} API allows interacting with the current computer.
 *
 * @cc.module os
 */
public class OSAPI implements ILuaAPI {
    private final IAPIEnvironment apiEnvironment;

    private final Int2ObjectMap<Alarm> alarms = new Int2ObjectOpenHashMap<>();
    private int clock;
    private double time;
    private int day;

    private int nextAlarmToken = 0;

    private record Alarm(double time, int day) implements Comparable<Alarm> {
        @Override
        public int compareTo(Alarm o) {
            var t = day * 24.0 + time;
            var ot = day * 24.0 + time;
            return Double.compare(t, ot);
        }
    }

    public OSAPI(IAPIEnvironment environment) {
        apiEnvironment = environment;
    }

    @Override
    public String[] getNames() {
        return new String[]{ "os" };
    }

    @Override
    public void startup() {
        time = apiEnvironment.getComputerEnvironment().getTimeOfDay();
        day = apiEnvironment.getComputerEnvironment().getDay();
        clock = 0;

        synchronized (alarms) {
            alarms.clear();
        }
    }

    @Override
    public void update() {
        clock++;

        // Wait for all of our alarms
        synchronized (alarms) {
            var previousTime = time;
            var previousDay = day;
            var time = apiEnvironment.getComputerEnvironment().getTimeOfDay();
            var day = apiEnvironment.getComputerEnvironment().getDay();

            if (time > previousTime || day > previousDay) {
                var now = this.day * 24.0 + this.time;
                Iterator<Int2ObjectMap.Entry<Alarm>> it = alarms.int2ObjectEntrySet().iterator();
                while (it.hasNext()) {
                    var entry = it.next();
                    var alarm = entry.getValue();
                    var t = alarm.day * 24.0 + alarm.time;
                    if (now >= t) {
                        apiEnvironment.queueEvent("alarm", entry.getIntKey());
                        it.remove();
                    }
                }
            }

            this.time = time;
            this.day = day;
        }
    }

    @Override
    public void shutdown() {
        synchronized (alarms) {
            alarms.clear();
        }
    }

    private static float getTimeForCalendar(Calendar c) {
        float time = c.get(Calendar.HOUR_OF_DAY);
        time += c.get(Calendar.MINUTE) / 60.0f;
        time += c.get(Calendar.SECOND) / (60.0f * 60.0f);
        return time;
    }

    private static int getDayForCalendar(Calendar c) {
        var g = c instanceof GregorianCalendar ? (GregorianCalendar) c : new GregorianCalendar();
        var year = c.get(Calendar.YEAR);
        var day = 0;
        for (var y = 1970; y < year; y++) {
            day += g.isLeapYear(y) ? 366 : 365;
        }
        day += c.get(Calendar.DAY_OF_YEAR);
        return day;
    }

    private static long getEpochForCalendar(Calendar c) {
        return c.getTimeInMillis();
    }

    /**
     * Adds an event to the event queue. This event can later be pulled with
     * os.pullEvent.
     *
     * @param name The name of the event to queue.
     * @param args The parameters of the event.
     * @cc.tparam string name The name of the event to queue.
     * @cc.param ... The parameters of the event. These can be any primitive type (boolean, number, string) as well as
    *                tables. Other types (like functions), as well as metatables, will not be preserved.
     * @cc.see os.pullEvent To pull the event queued
     */
    @LuaFunction
    public final void queueEvent(String name, IArguments args) throws LuaException {
        apiEnvironment.queueEvent(name, args.drop(1).getAll());
    }

    /**
     * Starts a timer that will run for the specified number of seconds. Once
     * the timer fires, a [`timer`] event will be added to the queue with the ID
     * returned from this function as the first parameter.
     * <p>
     * As with [sleep][`os.sleep`], the time will automatically be rounded up to
     * the nearest multiple of 0.05 seconds, as it waits for a fixed amount of
     * world ticks.
     *
     * @param time The number of seconds until the timer fires.
     * @return The ID of the new timer. This can be used to filter the [`timer`]
     * event, or {@linkplain #cancelTimer cancel the timer}.
     * @throws LuaException If the time is below zero.
     * @see #cancelTimer To cancel a timer.
     */
    @LuaFunction
    public final int startTimer(double time) throws LuaException {
        return apiEnvironment.startTimer(Math.round(checkFinite(0, time) / 0.05));
    }

    /**
     * Cancels a timer previously started with {@link #startTimer(double)}. This
     * will stop the timer from firing.
     *
     * @param token The ID of the timer to cancel.
     * @cc.since 1.6
     * @see #startTimer To start a timer.
     */
    @LuaFunction
    public final void cancelTimer(int token) {
        apiEnvironment.cancelTimer(token);
    }

    /**
     * Sets an alarm that will fire at the specified {@linkplain #time(IArguments) in-game time}.
     * When it fires, an {@code alarm} event will be added to the event queue with the
     * ID returned from this function as the first parameter.
     *
     * @param time The time at which to fire the alarm, in the range [0.0, 24.0).
     * @return The ID of the new alarm. This can be used to filter the
     * {@code alarm} event, or {@link #cancelAlarm cancel the alarm}.
     * @throws LuaException If the time is out of range.
     * @cc.since 1.2
     * @see #cancelAlarm To cancel an alarm.
     */
    @LuaFunction
    public final int setAlarm(double time) throws LuaException {
        checkFinite(0, time);
        if (time < 0.0 || time >= 24.0) throw new LuaException("Number out of range");
        synchronized (alarms) {
            var day = time > this.time ? this.day : this.day + 1;
            alarms.put(nextAlarmToken, new Alarm(time, day));
            return nextAlarmToken++;
        }
    }

    /**
     * Cancels an alarm previously started with setAlarm. This will stop the
     * alarm from firing.
     *
     * @param token The ID of the alarm to cancel.
     * @cc.since 1.6
     * @see #setAlarm To set an alarm.
     */
    @LuaFunction
    public final void cancelAlarm(int token) {
        synchronized (alarms) {
            alarms.remove(token);
        }
    }

    /**
     * Shuts down the computer immediately.
     */
    @LuaFunction("shutdown")
    public final void doShutdown() {
        apiEnvironment.shutdown();
    }

    /**
     * Reboots the computer immediately.
     */
    @LuaFunction("reboot")
    public final void doReboot() {
        apiEnvironment.reboot();
    }

    /**
     * Returns the ID of the computer.
     *
     * @return The ID of the computer.
     */
    @LuaFunction({ "getComputerID", "computerID" })
    public final int getComputerID() {
        return apiEnvironment.getComputerID();
    }

    /**
     * Returns the label of the computer, or {@code nil} if none is set.
     *
     * @return The label of the computer.
     * @cc.treturn string|nil The label of the computer.
     * @cc.since 1.3
     */
    @Nullable
    @LuaFunction({ "getComputerLabel", "computerLabel" })
    public final Object[] getComputerLabel() {
        var label = apiEnvironment.getLabel();
        return label == null ? null : new Object[]{ label };
    }

    /**
     * Set the label of this computer.
     *
     * @param label The new label. May be {@code nil} in order to clear it.
     * @cc.since 1.3
     */
    @LuaFunction
    public final void setComputerLabel(Optional<String> label) {
        apiEnvironment.setLabel(label.map(StringUtil::normaliseLabel).orElse(null));
    }

    /**
     * Returns the number of seconds that the computer has been running.
     *
     * @return The computer's uptime.
     * @cc.since 1.2
     */
    @LuaFunction
    public final double clock() {
        return clock * 0.05;
    }

    /**
     * Returns the current time depending on the string passed in. This will
     * always be in the range [0.0, 24.0).
     * <p>
     * * If called with {@code ingame}, the current world time will be returned.
     * This is the default if nothing is passed.
     * * If called with {@code utc}, returns the hour of the day in UTC time.
     * * If called with {@code local}, returns the hour of the day in the
     * timezone the server is located in.
     * <p>
     * This function can also be called with a table returned from {@link #date},
     * which will convert the date fields into a UNIX timestamp (number of
     * seconds since 1 January 1970).
     *
     * @param args The locale of the time, or a table filled by {@code os.date("*t")} to decode. Defaults to {@code ingame} locale if not specified.
     * @return The hour of the selected locale, or a UNIX timestamp from the table, depending on the argument passed in.
     * @throws LuaException If an invalid locale is passed.
     * @cc.tparam [opt] string|table locale The locale of the time, or a table filled by {@code os.date("*t")} to decode. Defaults to {@code ingame} locale if not specified.
     * @cc.see textutils.formatTime To convert times into a user-readable string.
     * @cc.usage Print the current in-game time.
     * <pre>{@code
     * textutils.formatTime(os.time())
     * }</pre>
     * @cc.since 1.2
     * @cc.changed 1.80pr1 Add support for getting the local and UTC time.
     * @cc.changed 1.82.0 Arguments are now case insensitive.
     * @cc.changed 1.83.0 {@link #time(IArguments)} now accepts table arguments and converts them to UNIX timestamps.
     * @see #date To get a date table that can be converted with this function.
     */
    @LuaFunction
    public final Object time(IArguments args) throws LuaException {
        var value = args.get(0);
        if (value instanceof Map) return LuaDateTime.fromTable((Map<?, ?>) value);

        var param = args.optString(0, "ingame");
        return switch (param.toLowerCase(Locale.ROOT)) {
            case "utc" -> getTimeForCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            case "local" -> getTimeForCalendar(Calendar.getInstance());
            case "ingame" -> time;
            default -> throw new LuaException("Unsupported operation");
        };
    }

    /**
     * Returns the day depending on the locale specified.
     * <p>
     * * If called with {@code ingame}, returns the number of days since the
     * world was created. This is the default.
     * * If called with {@code utc}, returns the number of days since 1 January
     * 1970 in the UTC timezone.
     * * If called with {@code local}, returns the number of days since 1
     * January 1970 in the server's local timezone.
     *
     * @param args The locale to get the day for. Defaults to {@code ingame} if not set.
     * @return The day depending on the selected locale.
     * @throws LuaException If an invalid locale is passed.
     * @cc.since 1.48
     * @cc.changed 1.82.0 Arguments are now case insensitive.
     */
    @LuaFunction
    public final int day(Optional<String> args) throws LuaException {
        return switch (args.orElse("ingame").toLowerCase(Locale.ROOT)) {
            case "utc" -> getDayForCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            case "local" -> getDayForCalendar(Calendar.getInstance());
            case "ingame" -> day;
            default -> throw new LuaException("Unsupported operation");
        };
    }

    /**
     * Returns the number of milliseconds since an epoch depending on the locale.
     * <p>
     * * If called with {@code ingame}, returns the number of *in-game* milliseconds since the
     * world was created. This is the default.
     * * If called with {@code utc}, returns the number of milliseconds since 1
     * January 1970 in the UTC timezone.
     * * If called with {@code local}, returns the number of milliseconds since 1
     * January 1970 in the server's local timezone.
     * <p>
     * > [!INFO]
     * > The {@code ingame} time zone assumes that one Minecraft day consists of 86,400,000
     * > milliseconds. Since one in-game day is much faster than a real day (20 minutes), this
     * > will change quicker than real time - one real second is equal to 72000 in-game
     * > milliseconds. If you wish to convert this value to real time, divide by 72000; to
     * > convert to ticks (where a day is 24000 ticks), divide by 3600.
     *
     * @param args The locale to get the milliseconds for. Defaults to {@code ingame} if not set.
     * @return The milliseconds since the epoch depending on the selected locale.
     * @throws LuaException If an invalid locale is passed.
     * @cc.since 1.80pr1
     * @cc.usage Get the current time and use {@link #date} to convert it to a table.
     * <pre>{@code
     * -- Dividing by 1000 converts it from milliseconds to seconds.
     * local time = os.epoch("local") / 1000
     * local time_table = os.date("*t", time)
     * print(textutils.serialize(time_table))
     * }</pre>
     */
    @LuaFunction
    public final long epoch(Optional<String> args) throws LuaException {
        switch (args.orElse("ingame").toLowerCase(Locale.ROOT)) {
            case "utc": {
                // Get utc epoch
                var c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                return getEpochForCalendar(c);
            }
            case "local": {
                // Get local epoch
                var c = Calendar.getInstance();
                return getEpochForCalendar(c);
            }
            case "ingame":
                // Get in-game epoch
                synchronized (alarms) {
                    return day * 86400000L + (long) (time * 3600000.0);
                }
            default:
                throw new LuaException("Unsupported operation");
        }
    }

    /**
     * Returns a date string (or table) using a specified format string and
     * optional time to format.
     * <p>
     * The format string takes the same formats as C's [strftime](http://www.cplusplus.com/reference/ctime/strftime/)
     * function. The format string can also be prefixed with an exclamation mark
     * ({@code !}) to use UTC time instead of the server's local timezone.
     * <p>
     * If the format is exactly {@code *t} (optionally prefixed with {@code !}), a
     * table will be returned instead. This table has fields for the year, month,
     * day, hour, minute, second, day of the week, day of the year, and whether
     * Daylight Savings Time is in effect. This table can be converted to a UNIX
     * timestamp (days since 1 January 1970) with {@link #date}.
     *
     * @param formatA The format of the string to return. This defaults to {@code %c}, which expands to a string similar to "Sat Dec 24 16:58:00 2011".
     * @param timeA   The time to convert to a string. This defaults to the current time.
     * @return The resulting format string.
     * @throws LuaException If an invalid format is passed.
     * @cc.since 1.83.0
     * @cc.usage Print the current date in a user-friendly string.
     * <pre>{@code
     * os.date("%A %d %B %Y") -- See the reference above!
     * }</pre>
     */
    @LuaFunction
    public final Object date(Optional<String> formatA, Optional<Long> timeA) throws LuaException {
        var format = formatA.orElse("%c");
        long time = timeA.orElseGet(() -> Instant.now().getEpochSecond());

        var instant = Instant.ofEpochSecond(time);
        ZonedDateTime date;
        ZoneOffset offset;
        if (format.startsWith("!")) {
            offset = ZoneOffset.UTC;
            date = ZonedDateTime.ofInstant(instant, offset);
            format = format.substring(1);
        } else {
            var id = ZoneId.systemDefault();
            offset = id.getRules().getOffset(instant);
            date = ZonedDateTime.ofInstant(instant, id);
        }

        if (format.equals("*t")) return LuaDateTime.toTable(date, offset, instant);

        var formatter = new DateTimeFormatterBuilder();
        LuaDateTime.format(formatter, format);
        // ROOT would be more sensible, but US appears more consistent with the default C locale
        // on Linux.
        return formatter.toFormatter(Locale.US).format(date);
    }

}
