// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration.libmultipart;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator;
import alexiil.mc.lib.multipart.api.MultipartHolder;
import alexiil.mc.lib.multipart.api.MultipartUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Creates a {@linkplain AbstractPart multipart} based on a {@link BlockPlaceContext}.
 *
 * @see MultipartCreator
 */
public interface PlacementMultipartCreator {
    /**
     * Create a new part.
     *
     * @param holder  The holder which is creating this part.
     * @param context The current block placement context.
     * @return The newly created part.
     */
    AbstractPart create(MultipartHolder holder, BlockPlaceContext context);

    /**
     * Attempt to place this part into the world.
     * <p>
     * This largely mirrors the logic in {@link BlockItem#place(BlockPlaceContext)}, but using
     * {@link MultipartUtil#offerNewPart(Level, BlockPos, MultipartCreator)} to place the new part instead.
     *
     * @param context The current placement context.
     * @return Whether the part was placed or not.
     */
    default InteractionResult placePart(BlockPlaceContext context) {
        var level = context.getLevel();
        var position = context.getClickedPos();

        var offer = MultipartUtil.offerNewPart(level, position, holder -> create(holder, context));
        if (offer == null) return InteractionResult.PASS;

        // Be careful to only apply this server-side. Multiparts send a bunch of network packets when created, which we
        // obviously don't want to do on the server!
        if (!level.isClientSide) offer.apply();

        // Approximate the block state from the placed part, and then fire all the appropriate events.
        var stack = context.getItemInHand();
        var blockState = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        var player = context.getPlayer();
        var sound = blockState.getSoundType();
        level.playSound(player, position, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
        level.gameEvent(GameEvent.BLOCK_PLACE, position, GameEvent.Context.of(player, blockState));
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
