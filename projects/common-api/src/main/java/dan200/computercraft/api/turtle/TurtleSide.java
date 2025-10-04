// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.turtle;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * An enum representing the two sides of the turtle that a turtle upgrade might reside.
 */
public enum TurtleSide implements StringRepresentable {
    /**
     * The turtle's left side (where the pickaxe usually is on a Wireless Mining Turtle).
     */
    LEFT("left"),

    /**
     * The turtle's right side (where the modem usually is on a Wireless Mining Turtle).
     */
    RIGHT("right");

    public static final Codec<TurtleSide> CODEC = StringRepresentable.fromEnum(TurtleSide::values);

    private final String name;

    TurtleSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
