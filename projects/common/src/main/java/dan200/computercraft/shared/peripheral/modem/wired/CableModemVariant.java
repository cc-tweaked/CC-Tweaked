// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.modem.wired;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;

public enum CableModemVariant implements StringRepresentable {
    None("none", null, false, false),
    DownOff("down_off", Direction.DOWN, false, false),
    UpOff("up_off", Direction.UP, false, false),
    NorthOff("north_off", Direction.NORTH, false, false),
    SouthOff("south_off", Direction.SOUTH, false, false),
    WestOff("west_off", Direction.WEST, false, false),
    EastOff("east_off", Direction.EAST, false, false),
    DownOn("down_on", Direction.DOWN, true, false),
    UpOn("up_on", Direction.UP, true, false),
    NorthOn("north_on", Direction.NORTH, true, false),
    SouthOn("south_on", Direction.SOUTH, true, false),
    WestOn("west_on", Direction.WEST, true, false),
    EastOn("east_on", Direction.EAST, true, false),
    DownOffPeripheral("down_off_peripheral", Direction.DOWN, false, true),
    UpOffPeripheral("up_off_peripheral", Direction.UP, false, true),
    NorthOffPeripheral("north_off_peripheral", Direction.NORTH, false, true),
    SouthOffPeripheral("south_off_peripheral", Direction.SOUTH, false, true),
    WestOffPeripheral("west_off_peripheral", Direction.WEST, false, true),
    EastOffPeripheral("east_off_peripheral", Direction.EAST, false, true),
    DownOnPeripheral("down_on_peripheral", Direction.DOWN, true, true),
    UpOnPeripheral("up_on_peripheral", Direction.UP, true, true),
    NorthOnPeripheral("north_on_peripheral", Direction.NORTH, true, true),
    SouthOnPeripheral("south_on_peripheral", Direction.SOUTH, true, true),
    WestOnPeripheral("west_on_peripheral", Direction.WEST, true, true),
    EastOnPeripheral("east_on_peripheral", Direction.EAST, true, true);

    private static final CableModemVariant[] VALUES = values();

    private final String name;
    private final @Nullable Direction facing;
    private final boolean modemOn, peripheralOn;

    CableModemVariant(String name, @Nullable Direction facing, boolean modemOn, boolean peripheralOn) {
        this.name = name;
        this.facing = facing;
        this.modemOn = modemOn;
        this.peripheralOn = peripheralOn;
        if (ordinal() != getIndex(facing, modemOn, peripheralOn)) throw new IllegalStateException("Mismatched ordinal");
    }

    public static CableModemVariant from(Direction facing) {
        return VALUES[1 + facing.get3DDataValue()];
    }

    private static int getIndex(@Nullable Direction facing, boolean modem, boolean peripheral) {
        var state = (modem ? 1 : 0) + (peripheral ? 2 : 0);
        return facing == null ? 0 : 1 + 6 * state + facing.get3DDataValue();
    }

    public static CableModemVariant from(@Nullable Direction facing, boolean modem, boolean peripheral) {
        return VALUES[getIndex(facing, modem, peripheral)];
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public @Nullable Direction getFacing() {
        return facing;
    }

    public boolean isModemOn() {
        return modemOn;
    }

    public boolean isPeripheralOn() {
        return peripheralOn;
    }

    @Override
    public String toString() {
        return name;
    }
}
