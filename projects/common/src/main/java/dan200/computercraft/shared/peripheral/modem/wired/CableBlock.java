// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.WaterloggableHelpers;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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

import javax.annotation.Nullable;
import java.util.EnumMap;

import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;
import static dan200.computercraft.shared.util.WaterloggableHelpers.getFluidStateForPlacement;

public class CableBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
    public static final EnumProperty<CableModemVariant> MODEM = EnumProperty.create("modem", CableModemVariant.class);
    public static final BooleanProperty CABLE = BooleanProperty.create("cable");

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    static final EnumMap<Direction, BooleanProperty> CONNECTIONS = Util.make(new EnumMap<>(Direction.class), m -> {
        m.put(Direction.DOWN, DOWN);
        m.put(Direction.UP, UP);
        m.put(Direction.NORTH, NORTH);
        m.put(Direction.SOUTH, SOUTH);
        m.put(Direction.WEST, WEST);
        m.put(Direction.EAST, EAST);
    });

    public CableBlock(Properties settings) {
        super(settings);

        registerDefaultState(getStateDefinition().any()
            .setValue(MODEM, CableModemVariant.None)
            .setValue(CABLE, false)
            .setValue(NORTH, false).setValue(SOUTH, false)
            .setValue(EAST, false).setValue(WEST, false)
            .setValue(UP, false).setValue(DOWN, false)
            .setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODEM, CABLE, NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED);
    }

    public static boolean canConnectIn(BlockState state, Direction direction) {
        return state.getValue(CableBlock.CABLE) && state.getValue(CableBlock.MODEM).getFacing() != direction;
    }

    public static boolean doesConnectVisually(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!state.getValue(CABLE)) return false;
        if (state.getValue(MODEM).getFacing() == direction) return true;
        return PlatformHelper.get().hasWiredElementIn(level, pos, direction);
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return CableShapes.getShape(state);
    }

    @ForgeOverride
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        playerWillDestroy(world, pos, state, player);
        if (onCustomDestroyBlock(state, world, pos, player)) {
            return false;
        }

        return world.setBlock(pos, fluid.createLegacyBlock(), world.isClientSide ? UPDATE_ALL_IMMEDIATE : UPDATE_ALL);
    }

    public boolean onCustomDestroyBlock(BlockState state, Level world, BlockPos pos, Player player) {
        if (!state.getValue(CABLE) || state.getValue(MODEM).getFacing() == null) return false;

        var hit = world.clip(new ClipContext(
            WorldUtil.getRayStart(player), WorldUtil.getRayEnd(player),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        if (hit.getType() != HitResult.Type.BLOCK) return false;

        var tile = world.getBlockEntity(pos);
        if (!(tile instanceof CableBlockEntity cable) || !tile.hasLevel()) return false;

        ItemStack item;
        BlockState newState;
        if (WorldUtil.isVecInside(CableShapes.getModemShape(state), hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()))) {
            newState = state.setValue(MODEM, CableModemVariant.None);
            item = new ItemStack(ModRegistry.Items.WIRED_MODEM.get());
        } else {
            newState = state.setValue(CABLE, false);
            item = new ItemStack(ModRegistry.Items.CABLE.get());
        }

        world.setBlockAndUpdate(pos, correctConnections(world, pos, newState));

        cable.connectionsChanged();
        if (!world.isClientSide && !player.getAbilities().instabuild) {
            Block.popResource(world, pos, item);
        }

        return true;
    }

    @Override
    @Deprecated
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        return state.getValue(CABLE) ? new ItemStack(ModRegistry.Items.CABLE.get()) : new ItemStack(ModRegistry.Items.WIRED_MODEM.get());
    }

    @ForgeOverride
    public ItemStack getCloneItemStack(BlockState state, @Nullable HitResult hit, BlockGetter world, BlockPos pos, Player player) {
        var modem = state.getValue(MODEM).getFacing();
        boolean cable = state.getValue(CABLE);

        // If we've only got one, just use that.
        if (!cable) return new ItemStack(ModRegistry.Items.WIRED_MODEM.get());
        if (modem == null) return new ItemStack(ModRegistry.Items.CABLE.get());

        // We've a modem and cable, so try to work out which one we're interacting with
        return hit != null && WorldUtil.isVecInside(CableShapes.getModemShape(state), hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()))
            ? new ItemStack(ModRegistry.Items.WIRED_MODEM.get())
            : new ItemStack(ModRegistry.Items.CABLE.get());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        var tile = world.getBlockEntity(pos);
        if (tile instanceof CableBlockEntity cable && cable.hasCable()) cable.connectionsChanged();
        super.setPlacedBy(world, pos, state, placer, stack);
    }

    @Override
    @Deprecated
    public FluidState getFluidState(BlockState state) {
        return WaterloggableHelpers.getFluidState(state);
    }

    @Override
    @Deprecated
    public BlockState updateShape(BlockState state, Direction side, BlockState otherState, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        WaterloggableHelpers.updateShape(state, level, pos);

        // Should never happen, but handle the case where we've no modem or cable.
        if (!state.getValue(CABLE) && state.getValue(MODEM) == CableModemVariant.None) {
            return getFluidState(state).createLegacyBlock();
        }

        // Pop our modem if needed.
        var dir = state.getValue(MODEM).getFacing();
        if (dir != null && dir.equals(side) && !ModemShapes.canSupport(level, otherPos, side.getOpposite())) {
            // If we've no cable, follow normal Minecraft logic and just remove the block.
            if (!state.getValue(CABLE)) return getFluidState(state).createLegacyBlock();

            // Otherwise remove the cable and drop the modem manually.
            state = state.setValue(CableBlock.MODEM, CableModemVariant.None);
            if (level instanceof Level actualLevel) {
                Block.popResource(actualLevel, pos, new ItemStack(ModRegistry.Items.WIRED_MODEM.get()));
            }

            if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) cable.scheduleConnectionsChanged();
        }

        var modem = state.getValue(MODEM);
        if (modem.getFacing() == side && modem.isPeripheralOn() && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.queueRefreshPeripheral();
        }

        return level instanceof Level actualLevel
            ? state.setValue(CONNECTIONS.get(side), doesConnectVisually(state, actualLevel, pos, side))
            : state;
    }

    @Override
    @Deprecated
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var facing = state.getValue(MODEM).getFacing();
        if (facing == null) return true;

        return ModemShapes.canSupport(world, pos.relative(facing), facing.getOpposite());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = defaultBlockState()
            .setValue(WATERLOGGED, getFluidStateForPlacement(context));

        if (context.getItemInHand().getItem() instanceof CableBlockItem.Cable) {
            var world = context.getLevel();
            var pos = context.getClickedPos();
            return correctConnections(world, pos, state.setValue(CABLE, true));
        } else {
            return state.setValue(MODEM, CableModemVariant.from(context.getClickedFace().getOpposite()));
        }
    }

    public static BlockState correctConnections(Level world, BlockPos pos, BlockState state) {
        if (state.getValue(CABLE)) {
            return state
                .setValue(NORTH, doesConnectVisually(state, world, pos, Direction.NORTH))
                .setValue(SOUTH, doesConnectVisually(state, world, pos, Direction.SOUTH))
                .setValue(EAST, doesConnectVisually(state, world, pos, Direction.EAST))
                .setValue(WEST, doesConnectVisually(state, world, pos, Direction.WEST))
                .setValue(UP, doesConnectVisually(state, world, pos, Direction.UP))
                .setValue(DOWN, doesConnectVisually(state, world, pos, Direction.DOWN));
        } else {
            return state
                .setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false);
        }
    }

    @Override
    @Deprecated
    public final InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isCrouching() || !player.mayBuild()) return InteractionResult.PASS;
        return world.getBlockEntity(pos) instanceof CableBlockEntity modem ? modem.use(player) : InteractionResult.PASS;
    }

    @Override
    @Deprecated
    public final void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos, boolean isMoving) {
        if (world.getBlockEntity(pos) instanceof CableBlockEntity modem) modem.neighborChanged(neighbourPos);
    }

    @ForgeOverride
    public final void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbour) {
        if (world.getBlockEntity(pos) instanceof CableBlockEntity modem) modem.neighborChanged(neighbour);
    }

    @Override
    @Deprecated
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        if (world.getBlockEntity(pos) instanceof CableBlockEntity modem) modem.blockTick();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModRegistry.BlockEntities.CABLE.get().create(pos, state);
    }
}
