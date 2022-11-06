/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.shared.ModRegistry;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.*;

public abstract class ItemBlockCable extends BlockItem {
    private String translationKey;

    public ItemBlockCable(BlockCable block, Properties settings) {
        super(block, settings);
    }

    boolean placeAt(Level world, BlockPos pos, BlockState state, Player player) {
        // TODO: Check entity collision.
        if (!state.canSurvive(world, pos)) return false;

        world.setBlock(pos, state, 3);
        var soundType = state.getBlock().getSoundType(state, world, pos, player);
        world.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

        var tile = world.getBlockEntity(pos);
        if (tile instanceof TileCable cable) {
            cable.modemChanged();
            cable.connectionsChanged();
        }

        return true;
    }

    boolean placeAtCorrected(Level world, BlockPos pos, BlockState state) {
        return placeAt(world, pos, correctConnections(world, pos, state), null);
    }

    @Override
    public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> list) {
        if (allowedIn(group)) list.add(new ItemStack(this));
    }

    @Nonnull
    @Override
    public String getDescriptionId() {
        if (translationKey == null) {
            translationKey = Util.makeDescriptionId("block", ForgeRegistries.ITEMS.getKey(this));
        }
        return translationKey;
    }

    public static class WiredModem extends ItemBlockCable {
        public WiredModem(BlockCable block, Properties settings) {
            super(block, settings);
        }

        @Nonnull
        @Override
        public InteractionResult place(BlockPlaceContext context) {
            var stack = context.getItemInHand();
            if (stack.isEmpty()) return InteractionResult.FAIL;

            var world = context.getLevel();
            var pos = context.getClickedPos();
            var existingState = world.getBlockState(pos);

            // Try to add a modem to a cable
            if (existingState.getBlock() == ModRegistry.Blocks.CABLE.get() && existingState.getValue(MODEM) == CableModemVariant.None) {
                var side = context.getClickedFace().getOpposite();
                var newState = existingState
                    .setValue(MODEM, CableModemVariant.from(side))
                    .setValue(CONNECTIONS.get(side), existingState.getValue(CABLE));
                if (placeAt(world, pos, newState, context.getPlayer())) {
                    stack.shrink(1);
                    return InteractionResult.SUCCESS;
                }
            }

            return super.place(context);
        }
    }

    public static class Cable extends ItemBlockCable {
        public Cable(BlockCable block, Properties settings) {
            super(block, settings);
        }

        @Nonnull
        @Override
        public InteractionResult place(BlockPlaceContext context) {
            var stack = context.getItemInHand();
            if (stack.isEmpty()) return InteractionResult.FAIL;

            var world = context.getLevel();
            var pos = context.getClickedPos();

            // Try to add a cable to a modem inside the block we're clicking on.
            var insidePos = pos.relative(context.getClickedFace().getOpposite());
            var insideState = world.getBlockState(insidePos);
            if (insideState.getBlock() == ModRegistry.Blocks.CABLE.get() && !insideState.getValue(BlockCable.CABLE)
                && placeAtCorrected(world, insidePos, insideState.setValue(BlockCable.CABLE, true))) {
                stack.shrink(1);
                return InteractionResult.SUCCESS;
            }

            // Try to add a cable to a modem adjacent to this block
            var existingState = world.getBlockState(pos);
            if (existingState.getBlock() == ModRegistry.Blocks.CABLE.get() && !existingState.getValue(BlockCable.CABLE)
                && placeAtCorrected(world, pos, existingState.setValue(BlockCable.CABLE, true))) {
                stack.shrink(1);
                return InteractionResult.SUCCESS;
            }

            return super.place(context);
        }
    }
}
