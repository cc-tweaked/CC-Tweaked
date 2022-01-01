/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.lua;

import dan200.computercraft.core.computer.TimeoutState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

/**
 * The result of executing an action on a machine.
 *
 * Errors should halt the machine and display the error to the user.
 *
 * @see ILuaMachine#loadBios(InputStream)
 * @see ILuaMachine#handleEvent(String, Object[])
 */
public final class MachineResult
{
    /**
     * A successful complete execution.
     */
    public static final MachineResult OK = new MachineResult( false, false, null );

    /**
     * A successful paused execution.
     */
    public static final MachineResult PAUSE = new MachineResult( false, true, null );

    /**
     * An execution which timed out.
     */
    public static final MachineResult TIMEOUT = new MachineResult( true, false, TimeoutState.ABORT_MESSAGE );

    /**
     * An error with no user-friendly error message.
     */
    public static final MachineResult GENERIC_ERROR = new MachineResult( true, false, null );

    private final boolean error;
    private final boolean pause;
    private final String message;

    private MachineResult( boolean error, boolean pause, String message )
    {
        this.pause = pause;
        this.message = message;
        this.error = error;
    }

    public static MachineResult error( @Nonnull String error )
    {
        return new MachineResult( true, false, error );
    }

    public static MachineResult error( @Nonnull Exception error )
    {
        return new MachineResult( true, false, error.getMessage() );
    }

    public boolean isError()
    {
        return error;
    }

    public boolean isPause()
    {
        return pause;
    }

    @Nullable
    public String getMessage()
    {
        return message;
    }
}
