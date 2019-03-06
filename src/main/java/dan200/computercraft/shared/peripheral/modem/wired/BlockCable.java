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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.VerticalEntityPosition;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.ViewableWorld;
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

    static final EnumMap<Direction, BooleanProperty> CONNECTIONS =
        new EnumMap<>( new ImmutableMap.Builder<Direction, BooleanProperty>()
            .put( Direction.DOWN, DOWN ).put( Direction.UP, UP )
            .put( Direction.NORTH, NORTH ).put( Direction.SOUTH, SOUTH )
            .put( Direction.WEST, WEST ).put( Direction.EAST, EAST )
            .build() );

    public BlockCable( Settings settings )
    {
        super( settings, TileCable.FACTORY );

        setDefaultState( getStateFactory().getDefaultState()
            .with( MODEM, CableModemVariant.None )
            .with( CABLE, false )
            .with( NORTH, false ).with( SOUTH, false )
            .with( EAST, false ).with( WEST, false )
            .with( UP, false ).with( DOWN, false )
            .with( WATERLOGGED, false )
        );
    }

    @Override
    protected void appendProperties( StateFactory.Builder<Block, BlockState> builder )
    {
        builder.with( MODEM, CABLE, NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED );
    }

    public static boolean canConnectIn( BlockState state, Direction direction )
    {
        return state.get( BlockCable.CABLE ) && state.get( BlockCable.MODEM ).getFacing() != direction;
    }

    public static boolean doesConnectVisually( BlockState state, BlockView world, BlockPos pos, Direction direction )
    {
        if( !state.get( CABLE ) ) return false;
        if( state.get( MODEM ).getFacing() == direction ) return true;
        return ComputerCraftAPI.getWiredElementAt( world, pos.offset( direction ), direction.getOpposite() ) != null;
    }

    @Override
    @Deprecated
    public VoxelShape getOutlineShape( BlockState state, BlockView world, BlockPos pos, VerticalEntityPosition position ) // TODO: getBoundingShape
    {
        return CableShapes.getShape( state );
    }


    /*
    @Override
    public boolean removedByPlayer( @Nonnull BlockState state, World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, boolean willHarvest )
    {
        PeripheralType type = getPeripheralType( world, pos );
        if( type == PeripheralType.WiredModemWithCable )
        {
            RayTraceResult hit = state.collisionRayTrace( world, pos, WorldUtil.getRayStart( player ), WorldUtil.getRayEnd( player ) );
            if( hit != null )
            {
                BlockEntity tile = world.getTileEntity( pos );
                if( tile != null && tile instanceof TileCable && tile.hasWorld() )
                {
                    TileCable cable = (TileCable) tile;

                    ItemStack item;

                    AxisAlignedBB bb = cable.getModemBounds();
                    if( WorldUtil.isVecInsideInclusive( bb, hit.hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) ) )
                    {
                        world.setBlockState( pos, state.with( MODEM, CableModemVariant.None ), 3 );
                        item = PeripheralItemFactory.create( PeripheralType.WiredModem, null, 1 );
                    }
                    else
                    {
                        world.setBlockState( pos, state.with( CABLE, BlockCableCableVariant.NONE ), 3 );
                        item = PeripheralItemFactory.create( PeripheralType.Cable, null, 1 );
                    }

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

        return super.removedByPlayer( state, world, pos, player, willHarvest );
    }
    */

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public ItemStack getPickStack( BlockView world, BlockPos pos, BlockState state )
    {
        Direction modem = state.get( MODEM ).getFacing();
        boolean cable = state.get( CABLE );

        // If we've no cable, we assume we're a modem.
        if( !cable ) return new ItemStack( ComputerCraft.Items.wiredModem );

        if( modem != null )
        {
            // If we've a modem and cable, try to work out which one we're interacting with
            BlockEntity tile = world.getBlockEntity( pos );
            HitResult hit = MinecraftClient.getInstance().hitResult; // TODO: Accept this as an argument
            if( tile instanceof TileCable && hit != null &&
                CableShapes.getModemShape( state ).getBoundingBox().contains( hit.getPos().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            )
            {
                return new ItemStack( ComputerCraft.Items.wiredModem );
            }
        }

        return new ItemStack( ComputerCraft.Items.cable );
    }

    @Override
    public void onPlaced( World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileCable )
        {
            TileCable cable = (TileCable) tile;
            if( cable.hasCable() ) cable.connectionsChanged();
        }

        super.onPlaced( world, pos, state, placer, stack );
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
    public BlockState getStateForNeighborUpdate( BlockState state, Direction side, BlockState otherState, IWorld world, BlockPos pos, BlockPos otherPos )
    {
        updateWaterloggedPostPlacement( state, world, pos );
        // Should never happen, but handle the case where we've no modem or cable.
        if( !state.get( CABLE ) && state.get( MODEM ) == CableModemVariant.None )
        {
            return getFluidState( state ).getBlockState();
        }

        if( side == state.get( MODEM ).getFacing() && !state.canPlaceAt( world, pos ) )
        {
            if( !state.get( CABLE ) ) return getFluidState( state ).getBlockState();

            /* TODO:
            BlockEntity entity = world.getBlockEntity( pos );
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
    public boolean canPlaceAt( BlockState state, ViewableWorld world, BlockPos pos )
    {
        Direction facing = state.get( MODEM ).getFacing();
        if( facing == null ) return true;

        BlockPos offsetPos = pos.offset( facing );
        BlockState offsetState = world.getBlockState( offsetPos );
        return Block.isFaceFullSquare( offsetState.getCollisionShape( world, offsetPos ), facing.getOpposite() ) && !method_9581( offsetState.getBlock() );
    }

    @Nullable
    @Override
    public BlockState getPlacementState( ItemPlacementContext context )
    {
        BlockState state = getDefaultState()
            .with( WATERLOGGED, getWaterloggedStateForPlacement( context ) );

        if( context.getItemStack().getItem() instanceof ItemBlockCable.Cable )
        {
            World world = context.getWorld();
            BlockPos pos = context.getBlockPos();
            return correctConnections( world, pos, state.with( CABLE, true ) );
        }
        else
        {
            return state.with( MODEM, CableModemVariant.from( context.getFacing().getOpposite() ) );
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
    public boolean hasBlockEntityBreakingRender( BlockState var1 )
    {
        return true;
    }
}
