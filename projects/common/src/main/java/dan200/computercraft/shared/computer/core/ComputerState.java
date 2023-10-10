// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.core;

import net.minecraft.util.StringRepresentable;

public enum ComputerState implements StringRepresentable {
    OFF("off", ""),
    ON("on", "_on"),
    BLINKING("blinking", "_blink");

    private final String name;
    private final String texture;

    ComputerState(String name, String texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getTexture() {
        return texture;
    }
}
