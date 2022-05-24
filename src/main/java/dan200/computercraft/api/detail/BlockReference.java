/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.detail;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A reference to a block in the world, used by block detail providers.
 */
public class BlockReference
{
    private final World world;
    private final BlockPos pos;
    private final BlockState state;
    private final TileEntity tile;

    public BlockReference( World world, BlockPos pos )
    {
        this.world = world;
        this.pos = pos;
        this.state = world.getBlockState( pos );
        this.tile = world.getBlockEntity( pos );
    }

    public BlockReference( World world, BlockPos pos, BlockState state, TileEntity tile )
    {
        this.world = world;
        this.pos = pos;
        this.state = state;
        this.tile = tile;
    }

    @Nonnull
    public World getWorld()
    {
        return world;
    }

    @Nonnull
    public BlockPos getPos()
    {
        return pos;
    }

    @Nonnull
    public BlockState getState()
    {
        return state;
    }

    @Nullable
    public TileEntity getTile()
    {
        return tile;
    }
}
