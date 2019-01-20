/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.shared.common.BlockDirectional;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockPeripheralBase extends BlockDirectional
{
    public BlockPeripheralBase()
    {
        super( Material.ROCK );
    }

    protected abstract IBlockState getDefaultBlockState( PeripheralType type, EnumFacing placedSide );

    protected abstract PeripheralType getPeripheralType( int damage );

    protected abstract PeripheralType getPeripheralType( IBlockState state );

    protected abstract TilePeripheralBase createTile( PeripheralType type );

    @Override
    public final boolean canPlaceBlockOnSide( @Nonnull World world, @Nonnull BlockPos pos, EnumFacing side )
    {
        return true; // ItemPeripheralBase handles this
    }

    @Nonnull
    @Override
    @Deprecated
    public final IBlockState getStateForPlacement( World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int damage, EntityLivingBase placer )
    {
        return getDefaultBlockState( getPeripheralType( damage ), side );
    }

    @Override
    public final TileGeneric createTile( IBlockState state )
    {
        return createTile( getPeripheralType( state ) );
    }

    @Override
    public final TileGeneric createTile( int damage )
    {
        return createTile( getPeripheralType( damage ) );
    }

    public final PeripheralType getPeripheralType( IBlockAccess world, BlockPos pos )
    {
        return getPeripheralType( world.getBlockState( pos ) );
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( @Nonnull IBlockState state, RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile instanceof TilePeripheralBase
            ? PeripheralItemFactory.create( (TilePeripheralBase) tile )
            : super.getPickBlock( state, target, world, pos, player );
    }
}
