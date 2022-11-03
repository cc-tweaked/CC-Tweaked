/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.core.Direction;

public enum MoveDirection {
    FORWARD,
    BACK,
    UP,
    DOWN;

    public Direction toWorldDir(ITurtleAccess turtle) {
        return switch (this) {
            case FORWARD -> turtle.getDirection();
            case BACK -> turtle.getDirection().getOpposite();
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }
}
