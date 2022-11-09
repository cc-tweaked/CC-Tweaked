/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;

public enum CableModemVariant implements StringRepresentable {
    None("none", null),
    DownOff("down_off", Direction.DOWN),
    UpOff("up_off", Direction.UP),
    NorthOff("north_off", Direction.NORTH),
    SouthOff("south_off", Direction.SOUTH),
    WestOff("west_off", Direction.WEST),
    EastOff("east_off", Direction.EAST),
    DownOn("down_on", Direction.DOWN),
    UpOn("up_on", Direction.UP),
    NorthOn("north_on", Direction.NORTH),
    SouthOn("south_on", Direction.SOUTH),
    WestOn("west_on", Direction.WEST),
    EastOn("east_on", Direction.EAST),
    DownOffPeripheral("down_off_peripheral", Direction.DOWN),
    UpOffPeripheral("up_off_peripheral", Direction.UP),
    NorthOffPeripheral("north_off_peripheral", Direction.NORTH),
    SouthOffPeripheral("south_off_peripheral", Direction.SOUTH),
    WestOffPeripheral("west_off_peripheral", Direction.WEST),
    EastOffPeripheral("east_off_peripheral", Direction.EAST),
    DownOnPeripheral("down_on_peripheral", Direction.DOWN),
    UpOnPeripheral("up_on_peripheral", Direction.UP),
    NorthOnPeripheral("north_on_peripheral", Direction.NORTH),
    SouthOnPeripheral("south_on_peripheral", Direction.SOUTH),
    WestOnPeripheral("west_on_peripheral", Direction.WEST),
    EastOnPeripheral("east_on_peripheral", Direction.EAST);

    private static final CableModemVariant[] VALUES = values();

    private final String name;
    private final @Nullable Direction facing;

    CableModemVariant(String name, @Nullable Direction facing) {
        this.name = name;
        this.facing = facing;
    }

    public static CableModemVariant from(Direction facing) {
        return facing == null ? None : VALUES[1 + facing.get3DDataValue()];
    }

    public static CableModemVariant from(@Nullable Direction facing, boolean modem, boolean peripheral) {
        var state = (modem ? 1 : 0) + (peripheral ? 2 : 0);
        return facing == null ? None : VALUES[1 + 6 * state + facing.get3DDataValue()];
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public @Nullable Direction getFacing() {
        return facing;
    }

    @Override
    public String toString() {
        return name;
    }
}
