/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.ItemPeripheralBase;
import net.minecraft.block.Block;

public class ItemWiredModemFull extends ItemPeripheralBase
{
    public ItemWiredModemFull( Block block )
    {
        super( block );
    }

    @Override
    public PeripheralType getPeripheralType( int damage )
    {
        return PeripheralType.WiredModemFull;
    }
}
