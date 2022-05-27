/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.detail;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A reference to a block in the world, used by block detail providers.
 *
 * @param level       The level the block exists in.
 * @param pos         The position of the block.
 * @param state       The block state at this position.
 * @param blockEntity The block entity at this position, if it exists.
 */
public record BlockReference(
    @Nonnull Level level,
    @Nonnull BlockPos pos,
    @Nonnull BlockState state,
    @Nullable BlockEntity blockEntity
)
{
    public BlockReference( Level level, BlockPos pos )
    {
        this( level, pos, level.getBlockState( pos ), level.getBlockEntity( pos ) );
    }
}
