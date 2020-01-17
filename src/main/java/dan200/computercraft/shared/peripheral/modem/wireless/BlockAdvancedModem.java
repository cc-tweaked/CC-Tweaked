/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemBounds;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BlockAdvancedModem extends BlockGeneric
{
    public static final PropertyDirection FACING = BlockDirectional.FACING;
    private static final PropertyBool ON = PropertyBool.create( "on" );

    public BlockAdvancedModem()
    {
        super( Material.ROCK );
        setHardness( 2.0f );
        setTranslationKey( "computercraft:advanced_modem" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
            .withProperty( ON, false )
        );
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, FACING, ON );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        return getDefaultState().withProperty( FACING, EnumFacing.byIndex( meta ) );
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        return state.getValue( FACING ).getIndex();
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        return state.withProperty( ON, tile instanceof TileAdvancedModem && ((TileAdvancedModem) tile).isOn() );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateForPlacement( World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer )
    {
        return getDefaultState().withProperty( FACING, facing.getOpposite() );
    }

    @Override
    protected TileGeneric createTile( IBlockState state )
    {
        return new TileAdvancedModem();
    }

    @Override
    protected TileGeneric createTile( int damage )
    {
        return new TileAdvancedModem();
    }

    @Override
    @Deprecated
    public final boolean isOpaqueCube( IBlockState state )
    {
        return false;
    }

    @Override
    @Deprecated
    public final boolean isFullCube( IBlockState state )
    {
        return false;
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape( IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side )
    {
        return BlockFaceShape.UNDEFINED;
    }

    @Nonnull
    @Override
    @Deprecated
    public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos )
    {
        return ModemBounds.getBounds( state.getValue( FACING ) );
    }
}
