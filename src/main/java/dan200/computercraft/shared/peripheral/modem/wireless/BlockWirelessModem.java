/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public class BlockWirelessModem extends BlockGeneric
{
    public static final AxisAlignedBB[] BOXES = new AxisAlignedBB[]{
        new AxisAlignedBB( 0.125, 0.0, 0.125, 0.875, 0.1875, 0.875 ), // Down
        new AxisAlignedBB( 0.125, 0.8125, 0.125, 0.875, 1.0, 0.875 ), // Up
        new AxisAlignedBB( 0.125, 0.125, 0.0, 0.875, 0.875, 0.1875 ), // North
        new AxisAlignedBB( 0.125, 0.125, 0.8125, 0.875, 0.875, 1.0 ), // South
        new AxisAlignedBB( 0.0, 0.125, 0.125, 0.1875, 0.875, 0.875 ), // West
        new AxisAlignedBB( 0.8125, 0.125, 0.125, 1.0, 0.875, 0.875 ), // East
    };

    public static final PropertyDirection FACING = BlockDirectional.FACING;
    public static final PropertyBool ON = PropertyBool.create( "on" );

    private final Supplier<TileWirelessModem> modem;

    public BlockWirelessModem( Supplier<TileWirelessModem> modem )
    {
        super( Material.ROCK );

        this.modem = modem;

        setHardness( 2.0f );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
            .withProperty( ON, false )
        );
    }

    @Nonnull
    @Override
    public String getTranslationKey()
    {
        return "tile." + getRegistryName();
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
        return getDefaultState()
            .withProperty( FACING, EnumFacing.byIndex( meta ) )
            .withProperty( ON, false );
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
        return state.withProperty( ON, tile instanceof TileWirelessModem && ((TileWirelessModem) tile).isModemOn() );
    }

    @Override
    public IBlockState getDefaultBlockState( EnumFacing placedSide )
    {
        EnumFacing dir = placedSide.getOpposite();
        return getDefaultState().withProperty( FACING, dir );
    }

    @Nonnull
    @Override
    @Deprecated
    public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess world, BlockPos pos )
    {
        int direction = state.getValue( FACING ).getIndex();
        return direction >= 0 && direction < BOXES.length ? BOXES[direction] : Block.FULL_BLOCK_AABB;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity( @Nonnull World world, @Nonnull IBlockState state )
    {
        return modem.get();
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
}
