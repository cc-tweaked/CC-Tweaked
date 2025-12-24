// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.lectern;

import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.pocket.core.PocketHolder;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.util.BlockEntityHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Extends {@link LecternBlock} with support for {@linkplain PrintoutItem printouts}.
 * <p>
 * Unlike the vanilla lectern, this block is never empty. If the book is removed from the lectern, it converts back to
 * its vanilla version (see {@link #clearLectern(Level, BlockPos, BlockState)}).
 *
 * @see PrintoutItem#useOn(UseOnContext) Placing books into a lectern.
 */
public class CustomLecternBlock extends LecternBlock {
    public CustomLecternBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HAS_BOOK, true));
    }

    /**
     * Attempt to place an item onto an (empty) lectern.
     *
     * @param player     The player placing the item.
     * @param level      The current level.
     * @param pos        The position of the lectern.
     * @param blockState The current state of the lectern.
     * @param item       The item to place in the custom lectern.
     * @return Whether the item was placed or not.
     */
    public static InteractionResult tryPlaceItem(Player player, Level level, BlockPos pos, BlockState blockState, ItemStack item) {
        if (item.getItem() instanceof PrintoutItem || item.getItem() instanceof PocketComputerItem) {
            if (!level.isClientSide()) replaceLectern(player, level, pos, blockState, item);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Replace a vanilla lectern with a custom one.
     *
     * @param player     The player placing the item.
     * @param level      The current level.
     * @param pos        The position of the lectern.
     * @param blockState The current state of the lectern.
     * @param item       The item to place in the custom lectern.
     */
    private static void replaceLectern(Player player, Level level, BlockPos pos, BlockState blockState, ItemStack item) {
        level.setBlockAndUpdate(pos, ModRegistry.Blocks.LECTERN.get().defaultBlockState()
            .setValue(HAS_BOOK, true)
            .setValue(FACING, blockState.getValue(FACING))
            .setValue(POWERED, blockState.getValue(POWERED)));

        if (level.getBlockEntity(pos) instanceof CustomLecternBlockEntity be) {
            be.setItem(item.consumeAndReturn(1, player));
        }
    }

    /**
     * Remove a custom lectern and replace it with an empty vanilla one.
     *
     * @param level      The current level.
     * @param pos        The position of the lectern.
     * @param blockState The current state of the lectern.
     */
    static void clearLectern(Level level, BlockPos pos, BlockState blockState) {
        level.setBlockAndUpdate(pos, Blocks.LECTERN.defaultBlockState()
            .setValue(HAS_BOOK, false)
            .setValue(FACING, blockState.getValue(FACING))
            .setValue(POWERED, blockState.getValue(POWERED)));
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Items.LECTERN);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CustomLecternBlockEntity lectern &&
            lectern.getItem().getItem() instanceof PocketComputerItem) {
            lectern.markRefreshPeripheral();
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // If we've no lectern, remove it.
        if (level.getBlockEntity(pos) instanceof CustomLecternBlockEntity lectern && lectern.getItem().isEmpty()) {
            clearLectern(level, pos, state);
            return;
        }

        super.tick(state, level, pos, random);
    }

    static void dropItem(Level level, BlockPos pos, BlockState state, ItemStack stack) {
        if (stack.isEmpty()) return;

        var direction = state.getValue(FACING);
        var dx = 0.25 * direction.getStepX();
        var dz = 0.25 * direction.getStepZ();
        var entity = new ItemEntity(level, pos.getX() + 0.5 + dx, pos.getY() + 1, pos.getZ() + 0.5 + dz, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);

        // If we're a pocket computer, update the holder and clear the peripheral.
        if (stack.getItem() instanceof PocketComputerItem pocket) {
            var brain = pocket.getBrain(new PocketHolder.ItemEntityHolder(entity), stack);
            if (brain != null) brain.computer().setPeripheral(ComputerSide.BOTTOM, null);
        }
    }

    @Override
    public CustomLecternBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomLecternBlockEntity(pos, state);
    }

    @Override
    @Deprecated
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof CustomLecternBlockEntity lectern ? lectern.getRedstoneSignal() : 0;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CustomLecternBlockEntity lectern) {
            if (player.isSecondaryUseActive()) {
                // When shift+clicked with an empty hand, drop the item and replace with the normal lectern.
                clearLectern(level, pos, state);
            } else {
                // Otherwise open the screen.
                lectern.openMenu(player);
            }

            player.awardStat(Stats.INTERACT_WITH_LECTERN);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : BlockEntityHelpers.createTickerHelper(type, ModRegistry.BlockEntities.LECTERN.get(), serverTicker);
    }

    private static final BlockEntityTicker<CustomLecternBlockEntity> serverTicker = (level, pos, state, lectern) -> lectern.tick();
}
