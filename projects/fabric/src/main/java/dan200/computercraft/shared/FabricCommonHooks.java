// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.shared.media.items.DiskItem;
import dan200.computercraft.shared.media.items.TreasureDiskItem;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class FabricCommonHooks {
    public static boolean onBlockDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        return !(state.getBlock() instanceof CableBlock cable) || !cable.onCustomDestroyBlock(state, level, pos, player);
    }

    /**
     * Allow placing disks/treasure disks into disk drives by clicking on them.
     *
     * @param player    The player placing the block.
     * @param level     The current level.
     * @param hand      The player's hand.
     * @param hitResult The hit collision.
     * @return Whether this interaction succeeded.
     * @see ServerPlayerGameMode#useItemOn(ServerPlayer, Level, ItemStack, InteractionHand, BlockHitResult) The original source of this logic.
     */
    public static InteractionResult useOnBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isSpectator()) return InteractionResult.PASS;

        var block = level.getBlockState(hitResult.getBlockPos());
        if (block.getBlock() != ModRegistry.Blocks.DISK_DRIVE.get()) return InteractionResult.PASS;

        if (player.isSecondaryUseActive() && doesSneakBypassUse(player.getMainHandItem()) && doesSneakBypassUse(player.getOffhandItem())) {
            var result = block.use(level, player, hand, hitResult);
            if (result.consumesAction()) return result;
        }

        return InteractionResult.PASS;
    }

    private static boolean doesSneakBypassUse(ItemStack stack) {
        return stack.isEmpty() || stack.getItem() instanceof DiskItem || stack.getItem() instanceof TreasureDiskItem;
    }
}
