/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.util.WaterloggableBlock;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReaderBase;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

public class BlockCable extends BlockGeneric implements WaterloggableBlock
{
    public static final EnumProperty<CableModemVariant> MODEM = EnumProperty.create( "modem", CableModemVariant.class );
    public static final BooleanProperty CABLE = BooleanProperty.create( "cable" );

    private static final BooleanProperty NORTH = BooleanProperty.create( "north" );
    private static final BooleanProperty SOUTH = BooleanProperty.create( "south" );
    private static final BooleanProperty EAST = BooleanProperty.create( "east" );
    private static final BooleanProperty WEST = BooleanProperty.create( "west" );
    private static final BooleanProperty UP = BooleanProperty.create( "up" );
    private static final BooleanProperty DOWN = BooleanProperty.create( "down" );

    static final EnumMap<EnumFacing, BooleanProperty> CONNECTIONS =
        new EnumMap<>( new ImmutableMap.Builder<EnumFacing, BooleanProperty>()
            .put( EnumFacing.DOWN, DOWN ).put( EnumFacing.UP, UP )
            .put( EnumFacing.NORTH, NORTH ).put( EnumFacing.SOUTH, SOUTH )
            .put( EnumFacing.WEST, WEST ).put( EnumFacing.EAST, EAST )
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
    protected void fillStateContainer( StateContainer.Builder<Block, IBlockState> builder )
    {
        builder.add( MODEM, CABLE, NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED );
    }

    public static boolean canConnectIn( IBlockState state, EnumFacing direction )
    {
        return state.get( BlockCable.CABLE ) && state.get( BlockCable.MODEM ).getFacing() != direction;
    }

    public static boolean doesConnectVisually( IBlockState state, IBlockReader world, BlockPos pos, EnumFacing direction )
    {
        if( !state.get( CABLE ) ) return false;
        if( state.get( MODEM ).getFacing() == direction ) return true;
        return ComputerCraftAPI.getWiredElementAt( world, pos.offset( direction ), direction.getOpposite() ) != null;
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape( IBlockState state, IBlockReader world, BlockPos pos )
    {
        return CableShapes.getShape( state );
    }

    @Override
    public boolean removedByPlayer( IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest, IFluidState fluid )
    {
        if( state.get( CABLE ) && state.get( MODEM ).getFacing() != null )
        {
            RayTraceResult hit = Block.collisionRayTrace( state, world, pos, WorldUtil.getRayStart( player ), WorldUtil.getRayEnd( player ) );
            if( hit != null )
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TileCable && tile.hasWorld() )
                {
                    TileCable cable = (TileCable) tile;

                    ItemStack item;
                    IBlockState newState;

                    VoxelShape bb = CableShapes.getModemShape( state );
                    if( WorldUtil.isVecInside( bb, hit.hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) ) )
                    {
                        item = new ItemStack( ComputerCraft.Items.wiredModem );
                        newState = state.with( MODEM, CableModemVariant.None );
                    }
                    else
                    {
                        newState = state.with( CABLE, false );
                        item = new ItemStack( ComputerCraft.Items.cable );
                    }

                    world.setBlockState( pos, correctConnections( world, pos, newState ), 3 );

                    cable.modemChanged();
                    cable.connectionsChanged();
                    if( !world.isRemote && !player.isCreative() )
                    {
                        Block.spawnAsEntity( world, pos, item );
                    }

                    return false;
                }
            }
        }

        return super.removedByPlayer( state, world, pos, player, willHarvest, fluid );
    }

    @Override
    public void getDrops( IBlockState state, NonNullList<ItemStack> drops, World world, BlockPos pos, int fortune )
    {
        if( state.get( CABLE ) ) drops.add( new ItemStack( ComputerCraft.Items.cable ) );
        if( state.get( MODEM ) != CableModemVariant.None ) drops.add( new ItemStack( ComputerCraft.Items.cable ) );
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( IBlockState state, RayTraceResult hit, IBlockReader world, BlockPos pos, EntityPlayer player )
    {
        EnumFacing modem = state.get( MODEM ).getFacing();
        boolean cable = state.get( CABLE );

        // If we've no cable, we assume we're a modem.
        if( !cable ) return new ItemStack( ComputerCraft.Items.wiredModem );

        if( modem != null )
        {
            // If we've a modem and cable, try to work out which one we're interacting with
            TileEntity tile = world.getTileEntity( pos );
            if( tile instanceof TileCable && hit != null &&
                CableShapes.getModemShape( state ).getBoundingBox().contains( hit.hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            )
            {
                return new ItemStack( ComputerCraft.Items.wiredModem );
            }
        }

        return new ItemStack( ComputerCraft.Items.cable );
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack )
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
        // Should never happen, but handle the case where we've no modem or cable.
        if( !state.get( CABLE ) && state.get( MODEM ) == CableModemVariant.None )
        {
            return getFluidState( state ).getBlockState();
        }

        if( side == state.get( MODEM ).getFacing() && !state.isValidPosition( world, pos ) )
        {
            if( !state.get( CABLE ) ) return getFluidState( state ).getBlockState();

            /* TODO:
            TileEntity entity = world.getTileEntity( pos );
            if( entity instanceof TileCable )
            {
                entity.modemChanged();
                entity.connectionsChanged();
            }
            */
            state = state.with( MODEM, CableModemVariant.None );
        }

        return state.with( CONNECTIONS.get( side ), doesConnectVisually( state, world, pos, side ) );
    }

    @Override
    @Deprecated
    public boolean isValidPosition( IBlockState state, IWorldReaderBase world, BlockPos pos )
    {
        EnumFacing facing = state.get( MODEM ).getFacing();
        if( facing == null ) return true;

        BlockPos offsetPos = pos.offset( facing );
        IBlockState offsetState = world.getBlockState( offsetPos );
        return offsetState.getBlockFaceShape( world, offsetPos, facing.getOpposite() ) == BlockFaceShape.SOLID;
    }

    @Nullable
    @Override
    public IBlockState getStateForPlacement( BlockItemUseContext context )
    {
        IBlockState state = getDefaultState()
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

    public static IBlockState correctConnections( World world, BlockPos pos, IBlockState state )
    {
        if( state.get( CABLE ) )
        {
            return state
                .with( NORTH, doesConnectVisually( state, world, pos, EnumFacing.NORTH ) )
                .with( SOUTH, doesConnectVisually( state, world, pos, EnumFacing.SOUTH ) )
                .with( EAST, doesConnectVisually( state, world, pos, EnumFacing.EAST ) )
                .with( WEST, doesConnectVisually( state, world, pos, EnumFacing.WEST ) )
                .with( UP, doesConnectVisually( state, world, pos, EnumFacing.UP ) )
                .with( DOWN, doesConnectVisually( state, world, pos, EnumFacing.DOWN ) );
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
    public final boolean isFullCube( IBlockState state )
    {
        return false;
    }

    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape( IBlockReader p_193383_1_, IBlockState p_193383_2_, BlockPos p_193383_3_, EnumFacing p_193383_4_ )
    {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @Deprecated
    public boolean hasCustomBreakingProgress( IBlockState state )
    {
        return true;
    }
}
