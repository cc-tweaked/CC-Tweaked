// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.mixin.ExplosionAccessor;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlock;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.platform.RegistryEntry;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.BlockEntityHelpers;
import dan200.computercraft.shared.util.WaterloggableHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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

import javax.annotation.Nullable;

import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;
import static dan200.computercraft.shared.util.WaterloggableHelpers.getFluidStateForPlacement;

public class TurtleBlock extends AbstractComputerBlock<TurtleBlockEntity> implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape DEFAULT_SHAPE = Shapes.box(
        0.125, 0.125, 0.125,
        0.875, 0.875, 0.875
    );

    private final BlockEntityTicker<TurtleBlockEntity> clientTicker = (level, pos, state, computer) -> computer.clientTick();

    public TurtleBlock(Properties settings, ComputerFamily family, RegistryEntry<BlockEntityType<TurtleBlockEntity>> type) {
        super(settings, family, type);
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    @Deprecated
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        var tile = world.getBlockEntity(pos);
        var offset = tile instanceof TurtleBlockEntity turtle ? turtle.getRenderOffset(1.0f) : Vec3.ZERO;
        return offset.equals(Vec3.ZERO) ? DEFAULT_SHAPE : DEFAULT_SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext placement) {
        return defaultBlockState()
            .setValue(FACING, placement.getHorizontalDirection())
            .setValue(WATERLOGGED, getFluidStateForPlacement(placement));
    }

    @Override
    @Deprecated
    public FluidState getFluidState(BlockState state) {
        return WaterloggableHelpers.getFluidState(state);
    }

    @Override
    @Deprecated
    public BlockState updateShape(BlockState state, Direction side, BlockState otherState, LevelAccessor world, BlockPos pos, BlockPos otherPos) {
        WaterloggableHelpers.updateShape(state, world, pos);
        return state;
    }

    @Override
    @Deprecated
    public final void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) return;

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TurtleBlockEntity turtle && !turtle.hasMoved()) {
            Containers.dropContents(level, pos, turtle);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);

        var tile = world.getBlockEntity(pos);
        if (!world.isClientSide && tile instanceof TurtleBlockEntity turtle) {
            if (entity instanceof Player player) turtle.setOwningPlayer(player.getGameProfile());

            if (stack.getItem() instanceof ITurtleItem item) {
                // Set Upgrades
                for (var side : TurtleSide.values()) {
                    turtle.getAccess().setUpgrade(side, item.getUpgrade(stack, side));
                }

                turtle.getAccess().setFuelLevel(item.getFuelLevel(stack));

                // Set colour
                var colour = item.getColour(stack);
                if (colour != -1) turtle.getAccess().setColour(colour);

                // Set overlay
                var overlay = item.getOverlay(stack);
                if (overlay != null) ((TurtleBrain) turtle.getAccess()).setOverlay(overlay);
            }
        }
    }

    @ForgeOverride
    public float getExplosionResistance(BlockState state, BlockGetter world, BlockPos pos, Explosion explosion) {
        var exploder = ((ExplosionAccessor) explosion).computercraft$getExploder();
        if (getFamily() == ComputerFamily.ADVANCED || exploder instanceof LivingEntity || exploder instanceof AbstractHurtingProjectile) {
            return 2000;
        }

        return explosionResistance;
    }

    @Override
    protected ItemStack getItem(AbstractComputerBlockEntity tile) {
        return tile instanceof TurtleBlockEntity turtle ? TurtleItemFactory.create(turtle) : ItemStack.EMPTY;
    }

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
        return level.isClientSide ? BlockEntityHelpers.createTickerHelper(type, this.type.get(), clientTicker) : super.getTicker(level, state, type);
    }
}
