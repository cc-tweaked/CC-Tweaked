// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.monitor;

import net.minecraft.core.Direction;

public record XYPair(float x, float y) {
    public XYPair add(float x, float y) {
        return new XYPair(this.x + x, this.y + y);
    }

    public static XYPair of(float xPos, float yPos, float zPos, Direction facing, Direction orientation) {
        return switch (orientation) {
            case NORTH -> switch (facing) {
                case NORTH -> new XYPair(1 - xPos, 1 - yPos);
                case SOUTH -> new XYPair(xPos, 1 - yPos);
                case WEST -> new XYPair(zPos, 1 - yPos);
                case EAST -> new XYPair(1 - zPos, 1 - yPos);
                default -> throw new IllegalStateException("Invalid facing " + facing);
            };
            case DOWN -> switch (facing) {
                case NORTH -> new XYPair(1 - xPos, zPos);
                case SOUTH -> new XYPair(xPos, 1 - zPos);
                case WEST -> new XYPair(zPos, xPos);
                case EAST -> new XYPair(1 - zPos, 1 - xPos);
                default -> throw new IllegalStateException("Invalid facing " + facing);
            };
            case UP -> switch (facing) {
                case NORTH -> new XYPair(1 - xPos, 1 - zPos);
                case SOUTH -> new XYPair(xPos, zPos);
                case WEST -> new XYPair(zPos, 1 - xPos);
                case EAST -> new XYPair(1 - zPos, xPos);
                default -> throw new IllegalStateException("Invalid facing " + facing);
            };
            default -> new XYPair(xPos, zPos);
        };
    }
}
