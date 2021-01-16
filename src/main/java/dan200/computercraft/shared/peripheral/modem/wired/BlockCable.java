/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

import static dan200.computercraft.shared.util.WaterloggableHelpers.*;

public class BlockCable extends BlockGeneric implements IWaterLoggable
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
        super( settings, Registry.ModTiles.CABLE );

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
    protected void createBlockStateDefinition( StateContainer.Builder<Block, BlockState> builder )
    {
        builder.add( MODEM, CABLE, NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED );
    }

    public static boolean canConnectIn( BlockState state, Direction direction )
    {
        return state.getValue( BlockCable.CABLE ) && state.getValue( BlockCable.MODEM ).getFacing() != direction;
    }

    public static boolean doesConnectVisually( BlockState state, IBlockReader world, BlockPos pos, Direction direction )
    {
        if( !state.getValue( CABLE ) ) return false;
        if( state.getValue( MODEM ).getFacing() == direction ) return true;
        return ComputerCraftAPI.getWiredElementAt( world, pos.relative( direction ), direction.getOpposite() ).isPresent();
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( @Nonnull BlockState state, @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull ISelectionContext context )
    {
        return CableShapes.getShape( state );
    }

    @Override
    public boolean removedByPlayer( BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid )
    {
        if( state.getValue( CABLE ) && state.getValue( MODEM ).getFacing() != null )
        {
            BlockRayTraceResult hit = world.clip( new RayTraceContext(
                WorldUtil.getRayStart( player ), WorldUtil.getRayEnd( player ),
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player
            ) );
            if( hit.getType() == RayTraceResult.Type.BLOCK )
            {
                TileEntity tile = world.getBlockEntity( pos );
                if( tile instanceof TileCable && tile.hasLevel() )
                {
                    TileCable cable = (TileCable) tile;

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
                    if( !world.isClientSide && !player.abilities.instabuild )
                    {
                        Block.popResource( world, pos, item );
                    }

                    return false;
                }
            }
        }

        return super.removedByPlayer( state, world, pos, player, willHarvest, fluid );
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( BlockState state, RayTraceResult hit, IBlockReader world, BlockPos pos, PlayerEntity player )
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
    public void setPlacedBy( World world, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer, @Nonnull ItemStack stack )
    {
        TileEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileCable )
        {
            TileCable cable = (TileCable) tile;
            if( cable.hasCable() ) cable.connectionsChanged();
        }

        super.setPlacedBy( world, pos, state, placer, stack );
    }

    @Nonnull
    @Override
    @Deprecated
    public FluidState getFluidState( @Nonnull BlockState state )
    {
        return getWaterloggedFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState updateShape( @Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState otherState, @Nonnull IWorld world, @Nonnull BlockPos pos, @Nonnull BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        // Should never happen, but handle the case where we've no modem or cable.
        if( !state.getValue( CABLE ) && state.getValue( MODEM ) == CableModemVariant.None )
        {
            return getFluidState( state ).createLegacyBlock();
        }

        return state.setValue( CONNECTIONS.get( side ), doesConnectVisually( state, world, pos, side ) );
    }

    @Override
    @Deprecated
    public boolean canSurvive( BlockState state, @Nonnull IWorldReader world, @Nonnull BlockPos pos )
    {
        Direction facing = state.getValue( MODEM ).getFacing();
        if( facing == null ) return true;

        return canSupportCenter( world, pos.relative( facing ), facing.getOpposite() );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( @Nonnull BlockItemUseContext context )
    {
        BlockState state = defaultBlockState()
            .setValue( WATERLOGGED, getWaterloggedStateForPlacement( context ) );

        if( context.getItemInHand().getItem() instanceof ItemBlockCable.Cable )
        {
            World world = context.getLevel();
            BlockPos pos = context.getClickedPos();
            return correctConnections( world, pos, state.setValue( CABLE, true ) );
        }
        else
        {
            return state.setValue( MODEM, CableModemVariant.from( context.getClickedFace().getOpposite() ) );
        }
    }

    public static BlockState correctConnections( World world, BlockPos pos, BlockState state )
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
