// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import static dan200.computercraft.shared.peripheral.modem.wired.CableBlock.*;

public abstract class CableBlockItem extends BlockItem {
    private @Nullable String translationKey;

    public CableBlockItem(CableBlock block, Properties settings) {
        super(block, settings);
    }

    boolean placeAt(Level world, BlockPos pos, BlockState state) {
        // TODO: Check entity collision.
        if (!state.canSurvive(world, pos)) return false;

        world.setBlockAndUpdate(pos, state);
        var soundType = state.getBlock().getSoundType(state);
        world.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

        var tile = world.getBlockEntity(pos);
        if (tile instanceof CableBlockEntity cable) cable.connectionsChanged();

        return true;
    }

    boolean placeAtCorrected(Level world, BlockPos pos, BlockState state) {
        return placeAt(world, pos, correctConnections(world, pos, state));
    }

    @Override
    public String getDescriptionId() {
        if (translationKey == null) {
            translationKey = Util.makeDescriptionId("block", RegistryWrappers.ITEMS.getKey(this));
        }
        return translationKey;
    }

    public static class WiredModem extends CableBlockItem {
        public WiredModem(CableBlock block, Properties settings) {
            super(block, settings);
        }

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
                if (placeAt(world, pos, newState)) {
                    stack.shrink(1);
                    return InteractionResult.sidedSuccess(world.isClientSide);
                }
            }

            return super.place(context);
        }
    }

    public static class Cable extends CableBlockItem {
        public Cable(CableBlock block, Properties settings) {
            super(block, settings);
        }

        @Override
        public InteractionResult place(BlockPlaceContext context) {
            var stack = context.getItemInHand();
            if (stack.isEmpty()) return InteractionResult.FAIL;

            var world = context.getLevel();
            var pos = context.getClickedPos();

            // Try to add a cable to a modem inside the block we're clicking on.
            var insidePos = pos.relative(context.getClickedFace().getOpposite());
            var insideState = world.getBlockState(insidePos);
            if (insideState.getBlock() == ModRegistry.Blocks.CABLE.get() && !insideState.getValue(CableBlock.CABLE)
                && placeAtCorrected(world, insidePos, insideState.setValue(CableBlock.CABLE, true))) {
                stack.shrink(1);
                return InteractionResult.sidedSuccess(world.isClientSide);
            }

            // Try to add a cable to a modem adjacent to this block
            var existingState = world.getBlockState(pos);
            if (existingState.getBlock() == ModRegistry.Blocks.CABLE.get() && !existingState.getValue(CableBlock.CABLE)
                && placeAtCorrected(world, pos, existingState.setValue(CableBlock.CABLE, true))) {
                stack.shrink(1);
                return InteractionResult.sidedSuccess(world.isClientSide);
            }

            return super.place(context);
        }
    }
}
