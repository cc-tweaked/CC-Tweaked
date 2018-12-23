/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.common.ITerminalTile;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;

public interface IComputerTile extends ITerminalTile
{
    void setComputerID( int id );

    void setLabel( String label );

    IComputer getComputer();

    IComputer createComputer();

    ComputerFamily getFamily();
}
