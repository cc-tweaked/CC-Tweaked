/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.blocks.BlockComputerBase;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.NamedBlockEntityType;
import dan200.computercraft.shared.util.WaterloggableBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockTurtle extends BlockComputerBase<TileTurtle> implements WaterloggableBlock
{
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape DEFAULT_SHAPE = VoxelShapes.cuboid(
        0.125, 0.125, 0.125,
        0.875, 0.875, 0.875
    );

    public BlockTurtle( Settings settings, ComputerFamily family, NamedBlockEntityType<TileTurtle> type )
    {
        super( settings, family, type );
        setDefaultState( getStateFactory().getDefaultState()
            .with( FACING, Direction.NORTH )
            .with( WATERLOGGED, false )
        );
    }

    @Override
    protected void appendProperties( StateFactory.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, WATERLOGGED );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockRenderType getRenderType( BlockState state )
    {
        return BlockRenderType.INVISIBLE;
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getOutlineShape( BlockState state, BlockView world, BlockPos pos, EntityContext position )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        Vec3d offset = tile instanceof TileTurtle ? ((TileTurtle) tile).getRenderOffset( 1.0f ) : Vec3d.ZERO;
        return offset.equals( Vec3d.ZERO ) ? DEFAULT_SHAPE : DEFAULT_SHAPE.offset( offset.x, offset.y, offset.z );
    }

    @Nullable
    @Override
    public BlockState getPlacementState( ItemPlacementContext placement )
    {
        return getDefaultState()
            .with( FACING, placement.getPlayerFacing() )
            .with( WATERLOGGED, getWaterloggedStateForPlacement( placement ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public FluidState getFluidState( BlockState state )
    {
        return getWaterloggedFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState getStateForNeighborUpdate( @Nonnull BlockState state, Direction side, BlockState otherState, IWorld world, BlockPos pos, BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        return state;
    }

    @Override
    public void onPlaced( World world, BlockPos pos, BlockState state, @Nullable LivingEntity player, @Nonnull ItemStack stack )
    {
        super.onPlaced( world, pos, state, player, stack );

        BlockEntity tile = world.getBlockEntity( pos );
        if( !world.isClient && tile instanceof TileTurtle )
        {
            TileTurtle turtle = (TileTurtle) tile;

            if( player instanceof PlayerEntity )
            {
                ((TileTurtle) tile).setOwningPlayer( ((PlayerEntity) player).getGameProfile() );
            }

            if( stack.getItem() instanceof ITurtleItem )
            {
                ITurtleItem item = (ITurtleItem) stack.getItem();

                // Set Upgrades
                for( TurtleSide side : TurtleSide.values() )
                {
                    turtle.getAccess().setUpgrade( side, item.getUpgrade( stack, side ) );
                }

                turtle.getAccess().setFuelLevel( item.getFuelLevel( stack ) );

                // Set colour
                int colour = item.getColour( stack );
                if( colour != -1 ) turtle.getAccess().setColour( colour );

                // Set overlay
                Identifier overlay = item.getOverlay( stack );
                if( overlay != null ) ((TurtleBrain) turtle.getAccess()).setOverlay( overlay );
            }
        }
    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileTurtle ? TurtleItemFactory.create( (TileTurtle) tile ) : ItemStack.EMPTY;
    }
}
