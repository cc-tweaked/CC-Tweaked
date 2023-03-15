// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.diskdrive;

import net.minecraft.util.StringRepresentable;

public enum DiskDriveState implements StringRepresentable {
    EMPTY("empty"),
    FULL("full"),
    INVALID("invalid");

    private final String name;

    DiskDriveState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
