package dan200.computercraft.api.network.wired;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Fetch or create an {@link IWiredElement} for a block at a given position.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerWiredProvider(IWiredProvider)
 * @see IWiredElementTile
 */
@FunctionalInterface
public interface IWiredProvider
{
    /**
     * Extract a wired network element from a block location.
     *
     * @param world The world the block is in.
     * @param pos   The position the block is at.
     * @param side  The side to get the network element from.
     * @return A network element, or {@code null} if there is not an element here you'd like to handle.
     */
    @Nullable
    IWiredElement getElement( @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side );
}
