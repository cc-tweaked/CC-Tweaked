/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.api.peripheral.IComputerAccess;

import javax.annotation.Nullable;

/**
 * {@link IComputerOwned} marks objects which are known to belong to a computer.
 *
 * The primary purpose of this is to allow Plethora (and potentially other mods) to run the various tracking methods
 * on {@link Computer}.
 *
 * You can generally assume {@link IComputerAccess} implements this interface, though you should always check first.
 *
 * @see dan200.computercraft.core.apis.ComputerAccess
 * @see dan200.computercraft.shared.peripheral.modem.wired.WiredModemPeripheral and the peripheral wrapper
 */
public interface IComputerOwned
{
    /**
     * Get the computer associated with this object
     *
     * @return The associated object, or {@code null} if none is known.
     */
    @Nullable
    Computer getComputer();
}
