package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PeripheralUtil
{
    public static IPeripheral getPeripheral( World world, BlockPos pos, EnumFacing side )
    {
        return world.isValid( pos ) && !world.isRemote ? ComputerCraft.getPeripheralAt( world, pos, side ) : null;
    }
}
