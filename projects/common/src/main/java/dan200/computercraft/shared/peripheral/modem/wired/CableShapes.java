// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.shared.peripheral.modem.ModemShapes;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;

import static dan200.computercraft.shared.peripheral.modem.wired.CableBlock.*;

public final class CableShapes {
    private static final double MIN = 0.375;
    private static final double MAX = 1 - MIN;

    private static final VoxelShape SHAPE_CABLE_CORE = Shapes.box(MIN, MIN, MIN, MAX, MAX, MAX);
    private static final EnumMap<Direction, VoxelShape> SHAPE_CABLE_ARM = Util.make(new EnumMap<>(Direction.class), m -> {
        m.put(Direction.DOWN, Shapes.box(MIN, 0, MIN, MAX, MIN, MAX));
        m.put(Direction.UP, Shapes.box(MIN, MAX, MIN, MAX, 1, MAX));
        m.put(Direction.NORTH, Shapes.box(MIN, MIN, 0, MAX, MAX, MIN));
        m.put(Direction.SOUTH, Shapes.box(MIN, MIN, MAX, MAX, MAX, 1));
        m.put(Direction.WEST, Shapes.box(0, MIN, MIN, MIN, MAX, MAX));
        m.put(Direction.EAST, Shapes.box(MAX, MIN, MIN, 1, MAX, MAX));
    });

    private static final VoxelShape[] SHAPES = new VoxelShape[(1 << 6) * 7];
    private static final VoxelShape[] CABLE_SHAPES = new VoxelShape[1 << 6];

    private CableShapes() {
    }

    private static int getCableIndex(BlockState state) {
        var index = 0;
        for (var facing : DirectionUtil.FACINGS) {
            if (state.getValue(CONNECTIONS.get(facing))) index |= 1 << facing.ordinal();
        }

        return index;
    }

    private static VoxelShape getCableShape(int index) {
        var shape = CABLE_SHAPES[index];
        if (shape != null) return shape;

        shape = SHAPE_CABLE_CORE;
        for (var facing : DirectionUtil.FACINGS) {
            if ((index & (1 << facing.ordinal())) != 0) {
                shape = Shapes.or(shape, SHAPE_CABLE_ARM.get(facing));
            }
        }

        return CABLE_SHAPES[index] = shape;
    }

    public static VoxelShape getCableShape(BlockState state) {
        if (!state.getValue(CABLE)) return Shapes.empty();
        return getCableShape(getCableIndex(state));
    }

    public static VoxelShape getModemShape(BlockState state) {
        var facing = state.getValue(MODEM).getFacing();
        return facing == null ? Shapes.empty() : ModemShapes.getBounds(facing);
    }

    public static VoxelShape getShape(BlockState state) {
        var facing = state.getValue(MODEM).getFacing();
        if (!state.getValue(CABLE)) return getModemShape(state);

        var cableIndex = getCableIndex(state);
        var index = cableIndex + ((facing == null ? 0 : facing.ordinal() + 1) << 6);

        var shape = SHAPES[index];
        if (shape != null) return shape;

        shape = getCableShape(cableIndex);
        if (facing != null) shape = Shapes.or(shape, ModemShapes.getBounds(facing));
        return SHAPES[index] = shape;
    }
}
