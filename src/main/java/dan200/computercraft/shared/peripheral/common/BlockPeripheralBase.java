/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.shared.common.BlockDirectional;
import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockPeripheralBase extends BlockDirectional
{
    public BlockPeripheralBase()
    {
        super( Material.ROCK );
    }

    protected abstract PeripheralType getPeripheralType( IBlockState state );

    @Override
    public final boolean canPlaceBlockOnSide( @Nonnull World world, @Nonnull BlockPos pos, EnumFacing side )
    {
        return true; // ItemPeripheralBase handles this
    }

    public final PeripheralType getPeripheralType( IBlockAccess world, BlockPos pos )
    {
        return getPeripheralType( world.getBlockState( pos ) );
    }
}
