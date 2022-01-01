/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Keeps track of tasks that a {@link Computer} should run on the main thread and how long that has been spent executing
 * them.
 *
 * This provides rate-limiting mechanism for tasks enqueued with {@link Computer#queueMainThread(Runnable)}, but also
 * those run elsewhere (such as during the turtle's tick - see {@link TurtleBrain#update()}). In order to handle this,
 * the executor goes through three stages:
 *
 * When {@link State#COOL}, the computer is allocated {@link ComputerCraft#maxMainComputerTime}ns to execute any work
 * this tick. At the beginning of the tick, we execute as many {@link MainThread} tasks as possible, until our
 * time-frame or the global time frame has expired.
 *
 * Then, when other objects (such as {@link BlockEntity}) are ticked, we update how much time we've used using
 * {@link IWorkMonitor#trackWork(long, TimeUnit)}.
 *
 * Now, if anywhere during this period, we use more than our allocated time slice, the executor is marked as
 * {@link State#HOT}. This means it will no longer be able to execute {@link MainThread} tasks (though will still
 * execute tile entity tasks, in order to prevent the main thread from exhausting work every tick).
 *
 * At the beginning of the next tick, we increment the budget e by {@link ComputerCraft#maxMainComputerTime} and any
 * {@link State#HOT} executors are marked as {@link State#COOLING}. They will remain cooling until their budget is
 * fully replenished (is equal to {@link ComputerCraft#maxMainComputerTime}). Note, this is different to
 * {@link MainThread}, which allows running when it has any budget left. When cooling, <em>no</em> tasks are executed -
 * be they on the tile entity or main thread.
 *
 * This mechanism means that, on average, computers will use at most {@link ComputerCraft#maxMainComputerTime}ns per
 * second, but one task source will not prevent others from executing.
 *
 * @see MainThread
 * @see IWorkMonitor
 * @see Computer#getMainThreadMonitor()
 * @see Computer#queueMainThread(Runnable)
 */
final class MainThreadExecutor implements IWorkMonitor
{
    /**
     * The maximum number of {@link MainThread} tasks allowed on the queue.
     */
    private static final int MAX_TASKS = 5000;

    private final Computer computer;

    /**
     * A lock used for any changes to {@link #tasks}, or {@link #onQueue}. This will be
     * used on the main thread, so locks should be kept as brief as possible.
     */
    private final Object queueLock = new Object();

    /**
     * The queue of tasks which should be executed.
     *
     * @see #queueLock
     */
    private final Queue<Runnable> tasks = new ArrayDeque<>( 4 );

    /**
     * Determines if this executor is currently present on the queue.
     *
     * This should be true iff {@link #tasks} is non-empty.
     *
     * @see #queueLock
     * @see #enqueue(Runnable)
     * @see #afterExecute(long)
     */
    volatile boolean onQueue;

    /**
     * The remaining budgeted time for this tick. This may be negative, in the case that we've gone over budget.
     *
     * @see #tickCooling()
     * @see #consumeTime(long)
     */
    private long budget = 0;

    /**
     * The last tick that {@link #budget} was updated.
     *
     * @see #tickCooling()
     * @see #consumeTime(long)
     */
    private int currentTick = -1;

    /**
     * The current state of this executor.
     *
     * @see #canWork()
     */
    private State state = State.COOL;

    private long pendingTime;

    long virtualTime;

    MainThreadExecutor( Computer computer )
    {
        this.computer = computer;
    }

    /**
     * Push a task onto this executor's queue, pushing it onto the {@link MainThread} if needed.
     *
     * @param runnable The task to run on the main thread.
     * @return Whether this task was enqueued (namely, was there space).
     */
    boolean enqueue( Runnable runnable )
    {
        synchronized( queueLock )
        {
            if( tasks.size() >= MAX_TASKS || !tasks.offer( runnable ) ) return false;
            if( !onQueue && state == State.COOL ) MainThread.queue( this, true );
            return true;
        }
    }

    void execute()
    {
        if( state != State.COOL ) return;

        Runnable task;
        synchronized( queueLock )
        {
            task = tasks.poll();
        }

        if( task != null ) task.run();
    }

    /**
     * Update the time taken to run an {@link #enqueue(Runnable)} task.
     *
     * @param time The time some task took to run.
     * @return Whether this should be added back to the queue.
     */
    boolean afterExecute( long time )
    {
        consumeTime( time );

        synchronized( queueLock )
        {
            virtualTime += time;
            updateTime();
            if( state != State.COOL || tasks.isEmpty() ) return onQueue = false;
            return true;
        }
    }

    /**
     * Whether we should execute "external" tasks (ones not part of {@link #tasks}).
     *
     * @return Whether we can execute external tasks.
     */
    @Override
    public boolean canWork()
    {
        return state != State.COOLING && MainThread.canExecute();
    }

    @Override
    public boolean shouldWork()
    {
        return state == State.COOL && MainThread.canExecute();
    }

    @Override
    public void trackWork( long time, @Nonnull TimeUnit unit )
    {
        long nanoTime = unit.toNanos( time );
        synchronized( queueLock )
        {
            pendingTime += nanoTime;
        }

        consumeTime( nanoTime );
        MainThread.consumeTime( nanoTime );
    }

    private void consumeTime( long time )
    {
        Tracking.addServerTiming( computer, time );

        // Reset the budget if moving onto a new tick. We know this is safe, as this will only have happened if
        // #tickCooling() isn't called, and so we didn't overrun the previous tick.
        if( currentTick != MainThread.currentTick() )
        {
            currentTick = MainThread.currentTick();
            budget = ComputerCraft.maxMainComputerTime;
        }

        budget -= time;

        // If we've gone over our limit, mark us as having to cool down.
        if( budget < 0 && state == State.COOL )
        {
            state = State.HOT;
            MainThread.cooling( this );
        }
    }

    /**
     * Move this executor forward one tick, replenishing the budget by {@link ComputerCraft#maxMainComputerTime}.
     *
     * @return Whether this executor has cooled down, and so is safe to run again.
     */
    boolean tickCooling()
    {
        state = State.COOLING;
        currentTick = MainThread.currentTick();
        budget = Math.min( budget + ComputerCraft.maxMainComputerTime, ComputerCraft.maxMainComputerTime );
        if( budget < ComputerCraft.maxMainComputerTime ) return false;

        state = State.COOL;
        synchronized( queueLock )
        {
            if( !tasks.isEmpty() && !onQueue ) MainThread.queue( this, false );
        }
        return true;
    }

    void updateTime()
    {
        virtualTime += pendingTime;
        pendingTime = 0;
    }

    private enum State
    {
        COOL,
        HOT,
        COOLING,
    }
}
