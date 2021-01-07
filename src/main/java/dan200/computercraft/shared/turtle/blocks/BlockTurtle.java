/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DamagingProjectileEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.*;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.util.WaterloggableHelpers.*;
import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class BlockTurtle extends BlockComputerBase<TileTurtle> implements IWaterLoggable
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape DEFAULT_SHAPE = VoxelShapes.create(
        0.125, 0.125, 0.125,
        0.875, 0.875, 0.875
    );

    public BlockTurtle( Properties settings, ComputerFamily family, RegistryObject<? extends TileEntityType<? extends TileTurtle>> type )
    {
        super( settings, family, type );
        setDefaultState( getStateContainer().getBaseState()
            .with( FACING, Direction.NORTH )
            .with( WATERLOGGED, false )
        );
    }

    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, WATERLOGGED );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockRenderType getRenderType( @Nonnull BlockState state )
    {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( @Nonnull BlockState state, IBlockReader world, @Nonnull BlockPos pos, @Nonnull ISelectionContext context )
    {
        TileEntity tile = world.getTileEntity( pos );
        Vec3d offset = tile instanceof TileTurtle ? ((TileTurtle) tile).getRenderOffset( 1.0f ) : Vec3d.ZERO;
        return offset.equals( Vec3d.ZERO ) ? DEFAULT_SHAPE : DEFAULT_SHAPE.withOffset( offset.x, offset.y, offset.z );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockItemUseContext placement )
    {
        return getDefaultState()
            .with( FACING, placement.getPlacementHorizontalFacing() )
            .with( WATERLOGGED, getWaterloggedStateForPlacement( placement ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public IFluidState getFluidState( @Nonnull BlockState state )
    {
        return getWaterloggedFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState updatePostPlacement( @Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState otherState, @Nonnull IWorld world, @Nonnull BlockPos pos, @Nonnull BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        return state;
    }

    @Override
    public void onBlockPlacedBy( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity player, @Nonnull ItemStack stack )
    {
        super.onBlockPlacedBy( world, pos, state, player, stack );

        TileEntity tile = world.getTileEntity( pos );
        if( !world.isRemote && tile instanceof TileTurtle )
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
                ResourceLocation overlay = item.getOverlay( stack );
                if( overlay != null ) ((TurtleBrain) turtle.getAccess()).setOverlay( overlay );
            }
        }
    }

    @Override
    public float getExplosionResistance( BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder, Explosion explosion )
    {
        if( getFamily() == ComputerFamily.ADVANCED || exploder instanceof LivingEntity || exploder instanceof DamagingProjectileEntity )
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
