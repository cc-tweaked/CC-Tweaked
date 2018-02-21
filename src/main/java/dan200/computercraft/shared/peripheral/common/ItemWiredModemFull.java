package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.shared.peripheral.PeripheralType;
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
