/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import javax.annotation.Nullable;

import dan200.computercraft.shared.common.BlockGeneric;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class BlockPrinter extends BlockGeneric {
    static final BooleanProperty TOP = BooleanProperty.of("top");
    static final BooleanProperty BOTTOM = BooleanProperty.of("bottom");
    private static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public BlockPrinter(Settings settings) {
        super(settings, TilePrinter.FACTORY);
        this.setDefaultState(this.getStateManager().getDefaultState()
                                 .with(FACING, Direction.NORTH)
                                 .with(TOP, false)
                                 .with(BOTTOM, false));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext placement) {
        return this.getDefaultState().with(FACING,
                                           placement.getPlayerFacing()
                                               .getOpposite());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (stack.hasCustomName()) {
            BlockEntity tileentity = world.getBlockEntity(pos);
            if (tileentity instanceof TilePrinter) {
                ((TilePrinter) tileentity).customName = stack.getName();
            }
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> properties) {
        super.appendProperties(properties);
        properties.add(FACING, TOP, BOTTOM);
    }
}
