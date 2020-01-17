/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import dan200.computercraft.shared.common.ITerminal;
import dan200.computercraft.shared.computer.blocks.ComputerState;

public interface IComputer extends ITerminal, InputHandler
{
    int getInstanceID();

    @Deprecated
    int getID();

    @Deprecated
    String getLabel();

    boolean isOn();

    boolean isCursorDisplayed();

    void turnOn();

    void shutdown();

    void reboot();

    @Override
    void queueEvent( String event, Object[] arguments );

    default void queueEvent( String event )
    {
        queueEvent( event, null );
    }

    default ComputerState getState()
    {
        if( !isOn() ) return ComputerState.Off;
        return isCursorDisplayed() ? ComputerState.Blinking : ComputerState.On;
    }
}
