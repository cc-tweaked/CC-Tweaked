/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.util.WaterloggableHelpers;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;
import static dan200.computercraft.shared.util.WaterloggableHelpers.getFluidStateForPlacement;

public class BlockCable extends BlockGeneric implements SimpleWaterloggedBlock
{
    public static final EnumProperty<CableModemVariant> MODEM = EnumProperty.create( "modem", CableModemVariant.class );
    public static final BooleanProperty CABLE = BooleanProperty.create( "cable" );

    private static final BooleanProperty NORTH = BooleanProperty.create( "north" );
    private static final BooleanProperty SOUTH = BooleanProperty.create( "south" );
    private static final BooleanProperty EAST = BooleanProperty.create( "east" );
    private static final BooleanProperty WEST = BooleanProperty.create( "west" );
    private static final BooleanProperty UP = BooleanProperty.create( "up" );
    private static final BooleanProperty DOWN = BooleanProperty.create( "down" );

    static final EnumMap<Direction, BooleanProperty> CONNECTIONS =
        new EnumMap<>( new ImmutableMap.Builder<Direction, BooleanProperty>()
            .put( Direction.DOWN, DOWN ).put( Direction.UP, UP )
            .put( Direction.NORTH, NORTH ).put( Direction.SOUTH, SOUTH )
            .put( Direction.WEST, WEST ).put( Direction.EAST, EAST )
            .build() );

    public BlockCable( Properties settings )
    {
        super( settings, Registry.ModBlockEntities.CABLE );

        registerDefaultState( getStateDefinition().any()
            .setValue( MODEM, CableModemVariant.None )
            .setValue( CABLE, false )
            .setValue( NORTH, false ).setValue( SOUTH, false )
            .setValue( EAST, false ).setValue( WEST, false )
            .setValue( UP, false ).setValue( DOWN, false )
            .setValue( WATERLOGGED, false )
        );
    }

    @Override
    protected void createBlockStateDefinition( StateDefinition.Builder<Block, BlockState> builder )
    {
        builder.add( MODEM, CABLE, NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED );
    }

    public static boolean canConnectIn( BlockState state, Direction direction )
    {
        return state.getValue( BlockCable.CABLE ) && state.getValue( BlockCable.MODEM ).getFacing() != direction;
    }

    public static boolean doesConnectVisually( BlockState state, BlockGetter world, BlockPos pos, Direction direction )
    {
        if( !state.getValue( CABLE ) ) return false;
        if( state.getValue( MODEM ).getFacing() == direction ) return true;
        return ComputerCraftAPI.getWiredElementAt( world, pos.relative( direction ), direction.getOpposite() ).isPresent();
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( @Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context )
    {
        return CableShapes.getShape( state );
    }

    @Override
    public boolean onDestroyedByPlayer( BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest, FluidState fluid )
    {
        if( state.getValue( CABLE ) && state.getValue( MODEM ).getFacing() != null )
        {
            BlockHitResult hit = world.clip( new ClipContext(
                WorldUtil.getRayStart( player ), WorldUtil.getRayEnd( player ),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
            ) );
            if( hit.getType() == HitResult.Type.BLOCK )
            {
                BlockEntity tile = world.getBlockEntity( pos );
                if( tile instanceof TileCable cable && tile.hasLevel() )
                {

                    ItemStack item;
                    BlockState newState;

                    if( WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getLocation().subtract( pos.getX(), pos.getY(), pos.getZ() ) ) )
                    {
                        newState = state.setValue( MODEM, CableModemVariant.None );
                        item = new ItemStack( Registry.ModItems.WIRED_MODEM.get() );
                    }
                    else
                    {
                        newState = state.setValue( CABLE, false );
                        item = new ItemStack( Registry.ModItems.CABLE.get() );
                    }

                    world.setBlock( pos, correctConnections( world, pos, newState ), 3 );

                    cable.modemChanged();
                    cable.connectionsChanged();
                    if( !world.isClientSide && !player.getAbilities().instabuild )
                    {
                        Block.popResource( world, pos, item );
                    }

                    return false;
                }
            }
        }

        return super.onDestroyedByPlayer( state, world, pos, player, willHarvest, fluid );
    }

    @Nonnull
    @Override
    public ItemStack getCloneItemStack( BlockState state, HitResult hit, BlockGetter world, BlockPos pos, Player player )
    {
        Direction modem = state.getValue( MODEM ).getFacing();
        boolean cable = state.getValue( CABLE );

        // If we've only got one, just use that.
        if( !cable ) return new ItemStack( Registry.ModItems.WIRED_MODEM.get() );
        if( modem == null ) return new ItemStack( Registry.ModItems.CABLE.get() );

        // We've a modem and cable, so try to work out which one we're interacting with
        return hit != null && WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getLocation().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? new ItemStack( Registry.ModItems.WIRED_MODEM.get() )
            : new ItemStack( Registry.ModItems.CABLE.get() );

    }

    @Override
    public void setPlacedBy( Level world, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer, @Nonnull ItemStack stack )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileCable cable )
        {
            if( cable.hasCable() ) cable.connectionsChanged();
        }

        super.setPlacedBy( world, pos, state, placer, stack );
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
        // Should never happen, but handle the case where we've no modem or cable.
        if( !state.getValue( CABLE ) && state.getValue( MODEM ) == CableModemVariant.None )
        {
            return getFluidState( state ).createLegacyBlock();
        }

        return state.setValue( CONNECTIONS.get( side ), doesConnectVisually( state, world, pos, side ) );
    }

    @Override
    @Deprecated
    public boolean canSurvive( BlockState state, @Nonnull LevelReader world, @Nonnull BlockPos pos )
    {
        Direction facing = state.getValue( MODEM ).getFacing();
        if( facing == null ) return true;

        return canSupportCenter( world, pos.relative( facing ), facing.getOpposite() );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( @Nonnull BlockPlaceContext context )
    {
        BlockState state = defaultBlockState()
            .setValue( WATERLOGGED, getFluidStateForPlacement( context ) );

        if( context.getItemInHand().getItem() instanceof ItemBlockCable.Cable )
        {
            Level world = context.getLevel();
            BlockPos pos = context.getClickedPos();
            return correctConnections( world, pos, state.setValue( CABLE, true ) );
        }
        else
        {
            return state.setValue( MODEM, CableModemVariant.from( context.getClickedFace().getOpposite() ) );
        }
    }

    public static BlockState correctConnections( Level world, BlockPos pos, BlockState state )
    {
        if( state.getValue( CABLE ) )
        {
            return state
                .setValue( NORTH, doesConnectVisually( state, world, pos, Direction.NORTH ) )
                .setValue( SOUTH, doesConnectVisually( state, world, pos, Direction.SOUTH ) )
                .setValue( EAST, doesConnectVisually( state, world, pos, Direction.EAST ) )
                .setValue( WEST, doesConnectVisually( state, world, pos, Direction.WEST ) )
                .setValue( UP, doesConnectVisually( state, world, pos, Direction.UP ) )
                .setValue( DOWN, doesConnectVisually( state, world, pos, Direction.DOWN ) );
        }
        else
        {
            return state
                .setValue( NORTH, false ).setValue( SOUTH, false ).setValue( EAST, false )
                .setValue( WEST, false ).setValue( UP, false ).setValue( DOWN, false );
        }
    }
}
