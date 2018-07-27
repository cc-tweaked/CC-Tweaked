/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.core.ComputerFamily;

import java.util.function.Supplier;

public class BlockCommandComputer extends BlockComputer
{
    public BlockCommandComputer( ComputerFamily family, Supplier<TileComputer> factory )
    {
        super( family, factory );
        setBlockUnbreakable();
        setResistance( 6000000.0F );
    }
}
