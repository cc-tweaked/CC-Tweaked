// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.lectern;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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
     * Replace a vanilla lectern with a custom one.
     *
     * @param level      The current level.
     * @param pos        The position of the lectern.
     * @param blockState The current state of the lectern.
     * @param item       The item to place in the custom lectern.
     */
    public static void replaceLectern(Level level, BlockPos pos, BlockState blockState, ItemStack item) {
        level.setBlockAndUpdate(pos, ModRegistry.Blocks.LECTERN.get().defaultBlockState()
            .setValue(HAS_BOOK, true)
            .setValue(FACING, blockState.getValue(FACING))
            .setValue(POWERED, blockState.getValue(POWERED)));

        if (level.getBlockEntity(pos) instanceof CustomLecternBlockEntity be) be.setItem(item.split(1));
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
    @Deprecated
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.LECTERN);
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

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) return;

        if (level.getBlockEntity(pos) instanceof CustomLecternBlockEntity lectern) {
            dropItem(level, pos, state, lectern.getItem().copy());
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void dropItem(Level level, BlockPos pos, BlockState state, ItemStack stack) {
        if (stack.isEmpty()) return;

        var direction = state.getValue(FACING);
        var dx = 0.25 * direction.getStepX();
        var dz = 0.25 * direction.getStepZ();
        var entity = new ItemEntity(level, pos.getX() + 0.5 + dx, pos.getY() + 1, pos.getZ() + 0.5 + dz, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    @Override
    public String getDescriptionId() {
        return Blocks.LECTERN.getDescriptionId();
    }

    @Override
    public CustomLecternBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomLecternBlockEntity(pos, state);
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CustomLecternBlockEntity lectern ? lectern.getRedstoneSignal() : 0;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CustomLecternBlockEntity lectern) {
            if (player.isSecondaryUseActive()) {
                // When shift+clicked with an empty hand, drop the item and replace with the normal lectern.
                clearLectern(level, pos, state);
            } else {
                // Otherwise open the screen.
                player.openMenu(lectern);
            }

            player.awardStat(Stats.INTERACT_WITH_LECTERN);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
