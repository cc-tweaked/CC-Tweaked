/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;

public interface IComputerTile
{
    int getComputerID();

    void setComputerID( int id );

    String getLabel();

    void setLabel( String label );

    ComputerFamily getFamily();

    @Deprecated
    IComputer getComputer();
}
