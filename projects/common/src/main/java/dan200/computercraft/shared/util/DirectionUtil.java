// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.util;

import dan200.computercraft.core.computer.ComputerSide;
import net.minecraft.core.Direction;

public final class DirectionUtil {
    private DirectionUtil() {
    }

    public static final Direction[] FACINGS = Direction.values();

    public static ComputerSide toLocal(Direction front, Direction dir) {
        if (front.getAxis() == Direction.Axis.Y) front = Direction.NORTH;

        if (dir == front) return ComputerSide.FRONT;
        if (dir == front.getOpposite()) return ComputerSide.BACK;
        if (dir == front.getCounterClockWise()) return ComputerSide.LEFT;
        if (dir == front.getClockWise()) return ComputerSide.RIGHT;
        if (dir == Direction.UP) return ComputerSide.TOP;
        return ComputerSide.BOTTOM;
    }

    public static float toPitchAngle(Direction dir) {
        return switch (dir) {
            case DOWN -> 90.0f;
            case UP -> 270.0f;
            default -> 0.0f;
        };
    }
}
