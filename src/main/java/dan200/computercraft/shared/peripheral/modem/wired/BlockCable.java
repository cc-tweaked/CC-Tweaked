/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
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
        super( settings, TileCable.FACTORY );

        setDefaultState( getStateContainer().getBaseState()
            .with( MODEM, CableModemVariant.None )
            .with( CABLE, false )
            .with( NORTH, false ).with( SOUTH, false )
            .with( EAST, false ).with( WEST, false )
            .with( UP, false ).with( DOWN, false )
            .with( WATERLOGGED, false )
        );
    }

    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, BlockState> builder )
    {
        builder.add( MODEM, CABLE, NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED );
    }

    public static boolean canConnectIn( BlockState state, Direction direction )
    {
        return state.get( BlockCable.CABLE ) && state.get( BlockCable.MODEM ).getFacing() != direction;
    }

    public static boolean doesConnectVisually( BlockState state, IBlockReader world, BlockPos pos, Direction direction )
    {
        if( !state.get( CABLE ) ) return false;
        if( state.get( MODEM ).getFacing() == direction ) return true;
        return ComputerCraftAPI.getWiredElementAt( world, pos.offset( direction ), direction.getOpposite() ).isPresent();
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context )
    {
        return CableShapes.getShape( state );
    }

    @Override
    public boolean removedByPlayer( BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, IFluidState fluid )
    {
        if( state.get( CABLE ) && state.get( MODEM ).getFacing() != null )
        {
            BlockRayTraceResult hit = world.rayTraceBlocks( new RayTraceContext(
                WorldUtil.getRayStart( player ), WorldUtil.getRayEnd( player ),
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player
            ) );
            if( hit.getType() == RayTraceResult.Type.BLOCK )
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TileCable && tile.hasWorld() )
                {
                    TileCable cable = (TileCable) tile;

                    ItemStack item;
                    BlockState newState;

                    if( WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getHitVec().subtract( pos.getX(), pos.getY(), pos.getZ() ) ) )
                    {
                        newState = state.with( MODEM, CableModemVariant.None );
                        item = new ItemStack( ComputerCraft.Items.wiredModem );
                    }
                    else
                    {
                        newState = state.with( CABLE, false );
                        item = new ItemStack( ComputerCraft.Items.cable );
                    }

                    world.setBlockState( pos, correctConnections( world, pos, newState ), 3 );

                    cable.modemChanged();
                    cable.connectionsChanged();
                    if( !world.isRemote && !player.abilities.isCreativeMode )
                    {
                        Block.spawnAsEntity( world, pos, item );
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
        Direction modem = state.get( MODEM ).getFacing();
        boolean cable = state.get( CABLE );

        // If we've only got one, just use that.
        if( !cable ) return new ItemStack( ComputerCraft.Items.wiredModem );
        if( modem == null ) return new ItemStack( ComputerCraft.Items.cable );

        // We've a modem and cable, so try to work out which one we're interacting with
        return hit != null && WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getHitVec().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? new ItemStack( ComputerCraft.Items.wiredModem )
            : new ItemStack( ComputerCraft.Items.cable );

    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileCable )
        {
            TileCable cable = (TileCable) tile;
            if( cable.hasCable() ) cable.connectionsChanged();
        }

        super.onBlockPlacedBy( world, pos, state, placer, stack );
    }

    @Nonnull
    @Override
    @Deprecated
    public IFluidState getFluidState( BlockState state )
    {
        return getWaterloggedFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState updatePostPlacement( @Nonnull BlockState state, Direction side, BlockState otherState, IWorld world, BlockPos pos, BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        // Should never happen, but handle the case where we've no modem or cable.
        if( !state.get( CABLE ) && state.get( MODEM ) == CableModemVariant.None )
        {
            return getFluidState( state ).getBlockState();
        }

        return state.with( CONNECTIONS.get( side ), doesConnectVisually( state, world, pos, side ) );
    }

    @Override
    @Deprecated
    public boolean isValidPosition( BlockState state, IWorldReader world, BlockPos pos )
    {
        Direction facing = state.get( MODEM ).getFacing();
        if( facing == null ) return true;

        BlockPos offsetPos = pos.offset( facing );
        BlockState offsetState = world.getBlockState( offsetPos );
        return hasSolidSide( offsetState, world, offsetPos, facing.getOpposite() );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockItemUseContext context )
    {
        BlockState state = getDefaultState()
            .with( WATERLOGGED, getWaterloggedStateForPlacement( context ) );

        if( context.getItem().getItem() instanceof ItemBlockCable.Cable )
        {
            World world = context.getWorld();
            BlockPos pos = context.getPos();
            return correctConnections( world, pos, state.with( CABLE, true ) );
        }
        else
        {
            return state.with( MODEM, CableModemVariant.from( context.getFace().getOpposite() ) );
        }
    }

    public static BlockState correctConnections( World world, BlockPos pos, BlockState state )
    {
        if( state.get( CABLE ) )
        {
            return state
                .with( NORTH, doesConnectVisually( state, world, pos, Direction.NORTH ) )
                .with( SOUTH, doesConnectVisually( state, world, pos, Direction.SOUTH ) )
                .with( EAST, doesConnectVisually( state, world, pos, Direction.EAST ) )
                .with( WEST, doesConnectVisually( state, world, pos, Direction.WEST ) )
                .with( UP, doesConnectVisually( state, world, pos, Direction.UP ) )
                .with( DOWN, doesConnectVisually( state, world, pos, Direction.DOWN ) );
        }
        else
        {
            return state
                .with( NORTH, false ).with( SOUTH, false ).with( EAST, false )
                .with( WEST, false ).with( UP, false ).with( DOWN, false );
        }
    }

    @Override
    @Deprecated
    public boolean hasCustomBreakingProgress( BlockState state )
    {
        return true;
    }
}
