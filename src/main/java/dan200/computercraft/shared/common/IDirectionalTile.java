/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import net.minecraft.util.EnumFacing;

public interface IDirectionalTile
{
    EnumFacing getDirection();

    void setDirection( EnumFacing dir );
}
