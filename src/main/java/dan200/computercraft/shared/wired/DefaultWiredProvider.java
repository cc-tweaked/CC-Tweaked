package dan200.computercraft.shared.wired;

import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredElementTile;
import dan200.computercraft.api.network.wired.IWiredProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DefaultWiredProvider implements IWiredProvider
{
    @Nullable
    @Override
    public IWiredElement getElement( @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side )
    {
        TileEntity te = world.getTileEntity( pos );
        return te instanceof IWiredElementTile ? ((IWiredElementTile) te).getWiredElement( side ) : null;
    }
}
