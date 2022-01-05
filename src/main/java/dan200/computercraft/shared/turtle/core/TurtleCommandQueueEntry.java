/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleCommand;

public class TurtleCommandQueueEntry
{
    public final int callbackID;
    public final ITurtleCommand command;

    public TurtleCommandQueueEntry( int callbackID, ITurtleCommand command )
    {
        this.callbackID = callbackID;
        this.command = command;
    }
}
