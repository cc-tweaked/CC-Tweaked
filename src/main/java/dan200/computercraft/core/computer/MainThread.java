/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.core.tracking.Tracking;

import java.util.ArrayDeque;
import java.util.Queue;

public class MainThread
{
    private static final int MAX_TASKS_PER_TICK = 1000;
    private static final int MAX_TASKS_TOTAL = 50000;

    private static final Queue<ITask> m_outstandingTasks = new ArrayDeque<>();
    private static final Object m_nextUnusedTaskIDLock = new Object();
    private static long m_nextUnusedTaskID = 0;

    public static long getUniqueTaskID()
    {
        synchronized( m_nextUnusedTaskIDLock )
        {
            return ++m_nextUnusedTaskID;
        }
    }

    public static boolean queueTask( ITask task )
    {
        synchronized( m_outstandingTasks )
        {
            if( m_outstandingTasks.size() < MAX_TASKS_TOTAL )
            {
                m_outstandingTasks.offer( task );
                return true;
            }
        }
        return false;
    }

    public static void executePendingTasks()
    {
        int tasksThisTick = 0;
        while( tasksThisTick < MAX_TASKS_PER_TICK )
        {
            ITask task = null;
            synchronized( m_outstandingTasks )
            {
                task = m_outstandingTasks.poll();
            }
            if( task != null )
            {
                long start = System.nanoTime();
                task.execute();

                long stop = System.nanoTime();
                Computer computer = task.getOwner();
                if( computer != null ) Tracking.addServerTiming( computer, stop - start );

                ++tasksThisTick;
            }
            else
            {
                break;
            }
        }
    }
}
