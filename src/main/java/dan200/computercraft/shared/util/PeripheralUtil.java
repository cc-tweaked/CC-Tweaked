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
        int y = pos.getY();
        if( y >= 0 && y < world.getHeight() && !world.isRemote )
        {
            return ComputerCraft.getPeripheralAt( world, pos, side );
        }
        return null;
    }
}
