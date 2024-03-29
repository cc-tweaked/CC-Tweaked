// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.util;

import dan200.computercraft.core.computer.ComputerSide;
import net.minecraft.core.Direction;

public final class DirectionUtil {
    private DirectionUtil() {
    }

    /**
     * A bitmask indicating all sides.
     */
    public static final int ALL_SIDES = (1 << 6) - 1;

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

    /**
     * Determine if a direction is in a bitmask.
     *
     * @param mask      The bitmask to test
     * @param direction The direction to check.
     * @return Whether the direction is in a bitmask.
     */
    public static boolean isSet(int mask, Direction direction) {
        return (mask & (1 << direction.ordinal())) != 0;
    }
}
