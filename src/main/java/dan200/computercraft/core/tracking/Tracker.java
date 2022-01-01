/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.tracking;

import dan200.computercraft.core.computer.Computer;

public interface Tracker
{
    /**
     * Report how long a task executed on the computer thread took.
     *
     * Computer thread tasks include events or a computer being turned on/off.
     *
     * @param computer The computer processing this task
     * @param time     The time taken for this task.
     */
    default void addTaskTiming( Computer computer, long time )
    {
    }

    /**
     * Report how long a task executed on the server thread took.
     *
     * Server tasks include actions performed by peripherals.
     *
     * @param computer The computer processing this task
     * @param time     The time taken for this task.
     */
    default void addServerTiming( Computer computer, long time )
    {
    }

    /**
     * Increment an arbitrary field by some value. Implementations may track how often this is called
     * as well as the change, to compute some level of "average".
     *
     * @param computer The computer to increment
     * @param field    The field to increment.
     * @param change   The amount to increment said field by.
     */
    default void addValue( Computer computer, TrackingField field, long change )
    {
    }
}
