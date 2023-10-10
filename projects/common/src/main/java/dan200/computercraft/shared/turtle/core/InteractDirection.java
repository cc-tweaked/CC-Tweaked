// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.core.Direction;

public enum InteractDirection {
    FORWARD,
    UP,
    DOWN;

    public Direction toWorldDir(ITurtleAccess turtle) {
        return switch (this) {
            case FORWARD -> turtle.getDirection();
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }
}
