// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlock;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.platform.RegistryEntry;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.util.BlockEntityHelpers;
import dan200.computercraft.shared.util.WaterloggableHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;
import static dan200.computercraft.shared.util.WaterloggableHelpers.getFluidStateForPlacement;

public class TurtleBlock extends AbstractComputerBlock<TurtleBlockEntity> implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * The explosion resistance to use when a turtle is "immune" to explosions.
     * <p>
     * This is used as the default explosion resistance for advanced turtles, and the resistance for entity-based
     * explosions (e.g. creepers).
     *
     * @see #getExplosionResistance(BlockState, BlockGetter, BlockPos, Explosion)
     */
    public static final float IMMUNE_EXPLOSION_RESISTANCE = 2000f;

    private static final VoxelShape DEFAULT_SHAPE = Shapes.box(
        0.125, 0.125, 0.125,
        0.875, 0.875, 0.875
    );

    private final BlockEntityTicker<TurtleBlockEntity> clientTicker = (level, pos, state, computer) -> computer.clientTick();

    public TurtleBlock(Properties settings, RegistryEntry<BlockEntityType<TurtleBlockEntity>> type) {
        super(settings, type);
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

        // Most blocks drop items and then remove the BE. However, if a turtle is consuming drops right now, that can
        // lead to loops where it tries to insert an item back into the inventory. To prevent this, take a reference to
        // the turtle BE now, remove it, and then drop the items.
        var turtle = !level.isClientSide && level.getBlockEntity(pos) instanceof TurtleBlockEntity t && !t.hasMoved()
            ? t : null;

        super.onRemove(state, level, pos, newState, isMoving);

        if (turtle != null) Containers.dropContents(level, pos, turtle);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);

        var tile = world.getBlockEntity(pos);
        if (!world.isClientSide && tile instanceof TurtleBlockEntity turtle) {
            if (entity instanceof Player player) turtle.setOwningPlayer(player.getGameProfile());

            if (stack.getItem() instanceof TurtleItem item) {
                // Set Upgrades
                for (var side : TurtleSide.values()) {
                    turtle.getAccess().setUpgradeWithData(side, item.getUpgradeWithData(stack, side));
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

    @Override
    @Deprecated
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var currentItem = player.getItemInHand(hand);
        if (currentItem.getItem() == Items.NAME_TAG && currentItem.hasCustomHoverName() && level.getBlockEntity(pos) instanceof AbstractComputerBlockEntity computer) {
            // Label to rename computer
            if (!level.isClientSide) {
                computer.setLabel(currentItem.getHoverName().getString());
                currentItem.shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @ForgeOverride
    public float getExplosionResistance(BlockState state, BlockGetter world, BlockPos pos, Explosion explosion) {
        var exploder = explosion.getDirectSourceEntity();
        if (exploder instanceof LivingEntity || exploder instanceof AbstractHurtingProjectile) {
            return IMMUNE_EXPLOSION_RESISTANCE;
        }

        return getExplosionResistance();
    }

    @Override
    protected ItemStack getItem(AbstractComputerBlockEntity tile) {
        if (!(tile instanceof TurtleBlockEntity turtle)) return ItemStack.EMPTY;
        if (!(asItem() instanceof TurtleItem item)) return ItemStack.EMPTY;

        var access = turtle.getAccess();
        return item.create(
            turtle.getComputerID(), turtle.getLabel(), access.getColour(),
            withPersistedData(access.getUpgradeWithData(TurtleSide.LEFT)),
            withPersistedData(access.getUpgradeWithData(TurtleSide.RIGHT)),
            access.getFuelLevel(), turtle.getOverlay()
        );
    }

    private static @Nullable UpgradeData<ITurtleUpgrade> withPersistedData(@Nullable UpgradeData<ITurtleUpgrade> upgrade) {
        return upgrade == null ? null : UpgradeData.of(upgrade.upgrade(), upgrade.upgrade().getPersistedData(upgrade.data()));
    }

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
        return level.isClientSide ? BlockEntityHelpers.createTickerHelper(type, this.type.get(), clientTicker) : super.getTicker(level, state, type);
    }
}
