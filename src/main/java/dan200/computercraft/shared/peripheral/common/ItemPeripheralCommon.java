package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.block.Block;

public class ItemPeripheralCommon extends ItemPeripheralBase
{
    private final PeripheralType peripheral;

    public ItemPeripheralCommon( Block block, PeripheralType peripheral )
    {
        super( block );
        this.peripheral = peripheral;
        setHasSubtypes( false );
    }

    @Override
    public PeripheralType getPeripheralType( int damage )
    {
        return peripheral;
    }
}
