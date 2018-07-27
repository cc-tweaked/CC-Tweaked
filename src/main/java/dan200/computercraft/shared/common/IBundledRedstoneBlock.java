package dan200.computercraft.shared.common;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

public interface IBundledRedstoneBlock
{
    /**
     * Get the bundled redstone output for a block. This will check
     * {@link Block#canConnectRedstone(IBlockState, IBlockAccess, BlockPos, EnumFacing)} before hand.
     *
     * @param state The block state for this block.
     * @param world The world this block exists in.
     * @param pos   The position this block exists at.
     * @param side  The side to get the output from. Note this is relative to this block, rather than the accessing block
     *              - this is inconsistent with {@link Block#getStrongPower(IBlockState, IBlockAccess, BlockPos, EnumFacing)}
     *              and the like.
     * @return The bundled output, or {@code 0} if none is available.
     */
    int getBundledRedstoneOutput( @Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side );
}
