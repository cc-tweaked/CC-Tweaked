/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import dan200.computercraft.shared.util.WaterloggableHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;
import static dan200.computercraft.shared.util.WaterloggableHelpers.getFluidStateForPlacement;

public class BlockTurtle extends BlockComputerBase<TileTurtle> implements SimpleWaterloggedBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape DEFAULT_SHAPE = Shapes.box(
        0.125, 0.125, 0.125,
        0.875, 0.875, 0.875
    );

    private final BlockEntityTicker<TileTurtle> clientTicker = ( level, pos, state, computer ) -> computer.clientTick();

    public BlockTurtle( Properties settings, ComputerFamily family, RegistryObject<BlockEntityType<TileTurtle>> type )
    {
        super( settings, family, type );
        registerDefaultState( getStateDefinition().any()
            .setValue( FACING, Direction.NORTH )
            .setValue( WATERLOGGED, false )
        );
    }

    @Override
    protected void createBlockStateDefinition( StateDefinition.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, WATERLOGGED );
    }

    @Nonnull
    @Override
    @Deprecated
    public RenderShape getRenderShape( @Nonnull BlockState state )
    {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( @Nonnull BlockState state, BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        Vec3 offset = tile instanceof TileTurtle turtle ? turtle.getRenderOffset( 1.0f ) : Vec3.ZERO;
        return offset.equals( Vec3.ZERO ) ? DEFAULT_SHAPE : DEFAULT_SHAPE.move( offset.x, offset.y, offset.z );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockPlaceContext placement )
    {
        return defaultBlockState()
            .setValue( FACING, placement.getHorizontalDirection() )
            .setValue( WATERLOGGED, getFluidStateForPlacement( placement ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public FluidState getFluidState( @Nonnull BlockState state )
    {
        return WaterloggableHelpers.getFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState updateShape( @Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState otherState, @Nonnull LevelAccessor world, @Nonnull BlockPos pos, @Nonnull BlockPos otherPos )
    {
        WaterloggableHelpers.updateShape( state, world, pos );
        return state;
    }

    @Override
    public void setPlacedBy( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity entity, @Nonnull ItemStack stack )
    {
        super.setPlacedBy( world, pos, state, entity, stack );

        BlockEntity tile = world.getBlockEntity( pos );
        if( !world.isClientSide && tile instanceof TileTurtle turtle )
        {
            if( entity instanceof Player player ) turtle.setOwningPlayer( player.getGameProfile() );

            if( stack.getItem() instanceof ITurtleItem item )
            {
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
    public float getExplosionResistance( BlockState state, BlockGetter world, BlockPos pos, Explosion explosion )
    {
        Entity exploder = explosion.getExploder();
        if( getFamily() == ComputerFamily.ADVANCED || exploder instanceof LivingEntity || exploder instanceof AbstractHurtingProjectile )
        {
            return 2000;
        }

        return super.getExplosionResistance( state, world, pos, explosion );
    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileTurtle turtle ? TurtleItemFactory.create( turtle ) : ItemStack.EMPTY;
    }

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker( @Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<U> type )
    {
        return level.isClientSide ? BaseEntityBlock.createTickerHelper( type, this.type.get(), clientTicker ) : super.getTicker( level, state, type );
    }
}
