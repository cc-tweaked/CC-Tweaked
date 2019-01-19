/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.util.EnumFacing;

public interface IPeripheralTile
{
    PeripheralType getPeripheralType();

    IPeripheral getPeripheral( EnumFacing side );

    default String getLabel()
    {
        return null;
    }
}
