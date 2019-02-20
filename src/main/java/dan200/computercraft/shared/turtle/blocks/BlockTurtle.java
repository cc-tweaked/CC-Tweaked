/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.shared.computer.blocks.BlockComputerBase;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.WaterloggableBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockTurtle extends BlockComputerBase<TileTurtle> implements WaterloggableBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape DEFAULT_SHAPE = VoxelShapes.create(
        0.125, 0.125, 0.125,
        0.875, 0.875, 0.875
    );

    public BlockTurtle( Properties settings, ComputerFamily family, TileEntityType<TileTurtle> type )
    {
        super( settings, family, type );
        setDefaultState( getStateContainer().getBaseState()
            .with( FACING, EnumFacing.NORTH )
            .with( WATERLOGGED, false )
        );
    }

    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, IBlockState> builder )
    {
        builder.add( FACING, WATERLOGGED );
    }

    @Nonnull
    @Override
    @Deprecated
    public EnumBlockRenderType getRenderType( IBlockState state )
    {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    @Deprecated
    public boolean isFullCube( IBlockState state )
    {
        return false;
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape( IBlockReader world, IBlockState state, BlockPos pos, EnumFacing side )
    {
        return BlockFaceShape.UNDEFINED;
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( IBlockState state, IBlockReader world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        Vec3d offset = tile instanceof TileTurtle ? ((TileTurtle) tile).getRenderOffset( 1.0f ) : Vec3d.ZERO;
        return offset.equals( Vec3d.ZERO ) ? DEFAULT_SHAPE : DEFAULT_SHAPE.withOffset( offset.x, offset.y, offset.z );
    }

    @Nullable
    @Override
    public IBlockState getStateForPlacement( BlockItemUseContext placement )
    {
        return getDefaultState()
            .with( FACING, placement.getPlacementHorizontalFacing() )
            .with( WATERLOGGED, getWaterloggedStateForPlacement( placement ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public IFluidState getFluidState( IBlockState state )
    {
        return getWaterloggedFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState updatePostPlacement( @Nonnull IBlockState state, EnumFacing side, IBlockState otherState, IWorld world, BlockPos pos, BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        return state;
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, @Nullable EntityLivingBase player, ItemStack itemStack )
    {
        super.onBlockPlacedBy( world, pos, state, player, itemStack );

        TileEntity tile = world.getTileEntity( pos );
        if( !world.isRemote && tile instanceof TileTurtle && player instanceof EntityPlayer )
        {
            ((TileTurtle) tile).setOwningPlayer( ((EntityPlayer) player).getGameProfile() );
        }
    }

    @Override
    public float getExplosionResistance( IBlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder, Explosion explosion )
    {
        if( getFamily() == ComputerFamily.Advanced && (exploder instanceof EntityLivingBase || exploder instanceof EntityFireball) )
        {
            return 2000;
        }

        return super.getExplosionResistance( state, world, pos, exploder, explosion );
    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileTurtle ? TurtleItemFactory.create( (TileTurtle) tile ) : ItemStack.EMPTY;
    }
}
