/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import dan200.computercraft.shared.common.ITerminal;

public interface IComputer extends ITerminal, InputHandler
{
    int getInstanceID();

    void turnOn();

    void shutdown();

    void reboot();

    default void queueEvent( String event )
    {
        queueEvent( event, null );
    }

    @Override
    void queueEvent( String event, Object[] arguments );

    default ComputerState getState()
    {
        if( !isOn() )
        {
            return ComputerState.OFF;
        }
        return isCursorDisplayed() ? ComputerState.BLINKING : ComputerState.ON;
    }

    boolean isOn();

    boolean isCursorDisplayed();
}
