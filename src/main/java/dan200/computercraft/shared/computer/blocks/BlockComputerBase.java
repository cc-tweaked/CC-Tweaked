/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.common.BlockDirectional;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockComputerBase extends BlockDirectional implements IBundledRedstoneBlock
{
    public BlockComputerBase( Material material )
    {
        super( material );
    }

    @Nonnull
    @Override
    public String getTranslationKey()
    {
        return "tile." + getRegistryName();
    }

    @Override
    public void onBlockAdded( World world, BlockPos pos, IBlockState state )
    {
        super.onBlockAdded( world, pos, state );
        updateInput( world, pos );
    }

    @Override
    public void setDirection( World world, BlockPos pos, EnumFacing dir )
    {
        super.setDirection( world, pos, dir );
        updateInput( world, pos );
    }

    protected abstract IBlockState getDefaultBlockState( EnumFacing placedSide );

    protected abstract TileComputerBase createTile();

    protected abstract ComputerFamily getFamily();

    @Override
    protected final IBlockState getDefaultBlockState( int damage, EnumFacing placedSide )
    {
        return getDefaultBlockState( placedSide );
    }

    @Override
    public final TileComputerBase createTile( IBlockState state )
    {
        return createTile();
    }

    @Override
    public final TileComputerBase createTile( int damage )
    {
        return createTile();
    }

    @Override
    @Deprecated
    public final boolean canProvidePower( IBlockState state )
    {
        return true;
    }

    @Override
    @Deprecated
    public final int getStrongPower( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing oppositeSide )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase && tile.hasWorld() )
        {
            TileComputerBase computer = (TileComputerBase) tile;
            return computer.getRedstoneOutput( oppositeSide.getOpposite() );
        }
        return 0;
    }

    @Override
    @Deprecated
    public final int getWeakPower( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing oppositeSide )
    {
        return getStrongPower( state, world, pos, oppositeSide );
    }

    @Override
    public int getBundledRedstoneOutput( @Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile instanceof TileGeneric && tile.hasWorld() ? ((TileComputerBase) tile).getBundledRedstoneOutput( side ) : 0;
    }

    protected void updateInput( IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase ) ((TileComputerBase) tile).updateInput();
    }
}
