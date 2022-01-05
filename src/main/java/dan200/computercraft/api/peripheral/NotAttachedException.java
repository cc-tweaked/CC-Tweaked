/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.peripheral;

/**
 * Thrown when performing operations on {@link IComputerAccess} when the current peripheral is no longer attached to
 * the computer.
 */
public class NotAttachedException extends IllegalStateException
{
    private static final long serialVersionUID = 1221244785535553536L;

    public NotAttachedException()
    {
        super( "You are not attached to this computer" );
    }

    public NotAttachedException( String s )
    {
        super( s );
    }
}
