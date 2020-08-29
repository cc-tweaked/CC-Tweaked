/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.CABLE;
import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.CONNECTIONS;
import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.MODEM;
import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.correctConnections;

import javax.annotation.Nonnull;

import dan200.computercraft.ComputerCraft;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public abstract class ItemBlockCable extends BlockItem {
    private String translationKey;

    public ItemBlockCable(BlockCable block, Item.Settings settings) {
        super(block, settings);
    }

    boolean placeAtCorrected(World world, BlockPos pos, BlockState state) {
        return this.placeAt(world, pos, correctConnections(world, pos, state));
    }

    boolean placeAt(World world, BlockPos pos, BlockState state) {
        if (!state.canPlaceAt(world, pos)) {
            return false;
        }

        world.setBlockState(pos, state, 3);
        BlockSoundGroup soundType = state.getBlock()
                                         .getSoundGroup(state);
        world.playSound(null, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileCable) {
            TileCable cable = (TileCable) tile;
            cable.modemChanged();
            cable.connectionsChanged();
        }

        return true;
    }

    @Override
    public String getTranslationKey() {
        if (this.translationKey == null) {
            this.translationKey = Util.createTranslationKey("block", Registry.ITEM.getId(this));
        }
        return this.translationKey;
    }

    @Override
    public void appendStacks(ItemGroup group, DefaultedList<ItemStack> list) {
        if (this.isIn(group)) {
            list.add(new ItemStack(this));
        }
    }

    public static class WiredModem extends ItemBlockCable {
        public WiredModem(BlockCable block, Settings settings) {
            super(block, settings);
        }

        @Nonnull
        @Override
        public ActionResult place(ItemPlacementContext context) {
            ItemStack stack = context.getStack();
            if (stack.isEmpty()) {
                return ActionResult.FAIL;
            }

            World world = context.getWorld();
            BlockPos pos = context.getBlockPos();
            BlockState existingState = world.getBlockState(pos);

            // Try to add a modem to a cable
            if (existingState.getBlock() == ComputerCraft.Blocks.cable && existingState.get(MODEM) == CableModemVariant.None) {
                Direction side = context.getSide()
                                        .getOpposite();
                BlockState newState = existingState.with(MODEM, CableModemVariant.from(side))
                                                   .with(CONNECTIONS.get(side), existingState.get(CABLE));
                if (this.placeAt(world, pos, newState)) {
                    stack.decrement(1);
                    return ActionResult.SUCCESS;
                }
            }

            return super.place(context);
        }
    }

    public static class Cable extends ItemBlockCable {
        public Cable(BlockCable block, Settings settings) {
            super(block, settings);
        }

        @Nonnull
        @Override
        public ActionResult place(ItemPlacementContext context) {
            ItemStack stack = context.getStack();
            if (stack.isEmpty()) {
                return ActionResult.FAIL;
            }

            World world = context.getWorld();
            BlockPos pos = context.getBlockPos();

            // Try to add a cable to a modem inside the block we're clicking on.
            BlockPos insidePos = pos.offset(context.getSide()
                                                   .getOpposite());
            BlockState insideState = world.getBlockState(insidePos);
            if (insideState.getBlock() == ComputerCraft.Blocks.cable && !insideState.get(BlockCable.CABLE) && this.placeAtCorrected(world,
                                                                                                                                    insidePos,
                                                                                                                                    insideState.with(BlockCable.CABLE,
                                                                                                                                                true))) {
                stack.decrement(1);
                return ActionResult.SUCCESS;
            }

            // Try to add a cable to a modem adjacent to this block
            BlockState existingState = world.getBlockState(pos);
            if (existingState.getBlock() == ComputerCraft.Blocks.cable && !existingState.get(BlockCable.CABLE) && this.placeAtCorrected(world,
                                                                                                                                        pos,
                                                                                                                                        existingState.with(
                                                                                                                                       BlockCable.CABLE,
                                                                                                                                       true))) {
                stack.decrement(1);
                return ActionResult.SUCCESS;
            }

            return super.place(context);
        }
    }
}
