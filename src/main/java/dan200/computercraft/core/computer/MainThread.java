/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaTask;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runs tasks on the main (server) thread, ticks {@link MainThreadExecutor}s, and limits how much time is used this
 * tick.
 *
 * Similar to {@link MainThreadExecutor}, the {@link MainThread} can be in one of three states: cool, hot and cooling.
 * However, the implementation here is a little different:
 *
 * {@link MainThread} starts cool, and runs as many tasks as it can in the current {@link #budget}ns. Any external tasks
 * (those run by tile entities, etc...) will also consume the budget
 *
 * Next tick, we put {@link ComputerCraft#maxMainGlobalTime} into our budget (and clamp it to that value to). If we're
 * still over budget, then we should not execute <em>any</em> work (either as part of {@link MainThread} or externally).
 */
public final class MainThread
{
    /**
     * An internal counter for {@link ILuaTask} ids.
     *
     * @see dan200.computercraft.api.lua.ILuaContext#issueMainThreadTask(ILuaTask)
     * @see #getUniqueTaskID()
     */
    private static final AtomicLong lastTaskId = new AtomicLong();

    /**
     * The queue of {@link MainThreadExecutor}s with tasks to perform.
     */
    private static final TreeSet<MainThreadExecutor> executors = new TreeSet<>( ( a, b ) -> {
        if( a == b ) return 0; // Should never happen, but let's be consistent here

        long at = a.virtualTime, bt = b.virtualTime;
        if( at == bt ) return Integer.compare( a.hashCode(), b.hashCode() );
        return at < bt ? -1 : 1;
    } );

    /**
     * The set of executors which went over budget in a previous tick, and are waiting for their time to run down.
     *
     * @see MainThreadExecutor#tickCooling()
     * @see #cooling(MainThreadExecutor)
     */
    private static final HashSet<MainThreadExecutor> cooling = new HashSet<>();

    /**
     * The current tick number. This is used by {@link MainThreadExecutor} to determine when to reset its own time
     * counter.
     *
     * @see #currentTick()
     */
    private static int currentTick;

    /**
     * The remaining budgeted time for this tick. This may be negative, in the case that we've gone over budget.
     */
    private static long budget;

    /**
     * Whether we should be executing any work this tick.
     *
     * This is true iff {@code MAX_TICK_TIME - currentTime} was true <em>at the beginning of the tick</em>.
     */
    private static boolean canExecute = true;

    private static long minimumTime = 0;

    private MainThread() {}

    public static long getUniqueTaskID()
    {
        return lastTaskId.incrementAndGet();
    }

    static void queue( @Nonnull MainThreadExecutor executor, boolean sleeper )
    {
        synchronized( executors )
        {
            if( executor.onQueue ) throw new IllegalStateException( "Cannot queue already queued executor" );
            executor.onQueue = true;
            executor.updateTime();

            // We're not currently on the queue, so update its current execution time to
            // ensure it's at least as high as the minimum.
            long newRuntime = minimumTime;

            // Slow down new computers a little bit.
            if( executor.virtualTime == 0 ) newRuntime += ComputerCraft.maxMainComputerTime;

            executor.virtualTime = Math.max( newRuntime, executor.virtualTime );

            executors.add( executor );
        }
    }

    static void cooling( @Nonnull MainThreadExecutor executor )
    {
        cooling.add( executor );
    }

    static void consumeTime( long time )
    {
        budget -= time;
    }

    static boolean canExecute()
    {
        return canExecute;
    }

    static int currentTick()
    {
        return currentTick;
    }

    public static void executePendingTasks()
    {
        // Move onto the next tick and cool down the global executor. We're allowed to execute if we have _any_ time
        // allocated for this tick. This means we'll stick much closer to doing MAX_TICK_TIME work every tick.
        //
        // Of course, we'll go over the MAX_TICK_TIME most of the time, but eventually that overrun will accumulate
        // and we'll skip a whole tick - bringing the average back down again.
        currentTick++;
        budget = Math.min( budget + ComputerCraft.maxMainGlobalTime, ComputerCraft.maxMainGlobalTime );
        canExecute = budget > 0;

        // Cool down any warm computers.
        cooling.removeIf( MainThreadExecutor::tickCooling );

        if( !canExecute ) return;

        // Run until we meet the deadline.
        long start = System.nanoTime();
        long deadline = start + budget;
        while( true )
        {
            MainThreadExecutor executor;
            synchronized( executors )
            {
                executor = executors.pollFirst();
            }
            if( executor == null ) break;

            long taskStart = System.nanoTime();
            executor.execute();

            long taskStop = System.nanoTime();
            synchronized( executors )
            {
                if( executor.afterExecute( taskStop - taskStart ) ) executors.add( executor );

                // Compute the new minimum time (including the next task on the queue too). Note that this may also include
                // time spent in external tasks.
                long newMinimum = executor.virtualTime;
                if( !executors.isEmpty() )
                {
                    MainThreadExecutor next = executors.first();
                    if( next.virtualTime < newMinimum ) newMinimum = next.virtualTime;
                }
                minimumTime = Math.max( minimumTime, newMinimum );
            }

            if( taskStop >= deadline ) break;
        }

        consumeTime( System.nanoTime() - start );
    }

    public static void reset()
    {
        currentTick = 0;
        budget = 0;
        canExecute = true;
        minimumTime = 0;
        lastTaskId.set( 0 );
        cooling.clear();
        synchronized( executors )
        {
            executors.clear();
        }
    }
}
