// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.util;

import javax.annotation.Nullable;

public enum Colour {
    BLACK(0x111111),
    RED(0xcc4c4c),
    GREEN(0x57A64E),
    BROWN(0x7f664c),
    BLUE(0x3366cc),
    PURPLE(0xb266e5),
    CYAN(0x4c99b2),
    LIGHT_GREY(0x999999),
    GREY(0x4c4c4c),
    PINK(0xf2b2cc),
    LIME(0x7fcc19),
    YELLOW(0xdede6c),
    LIGHT_BLUE(0x99b2f2),
    MAGENTA(0xe57fd8),
    ORANGE(0xf2b233),
    WHITE(0xf0f0f0);

    public static final Colour[] VALUES = values();

    public static Colour fromInt(int colour) {
        return Colour.VALUES[colour];
    }

    @Nullable
    public static Colour fromHex(int colour) {
        for (var entry : VALUES) {
            if (entry.getHex() == colour) return entry;
        }

        return null;
    }

    private final int hex;
    private final float red, green, blue;

    Colour(int hex) {
        this.hex = hex;
        red = ((hex >> 16) & 0xFF) / 255.0f;
        green = ((hex >> 8) & 0xFF) / 255.0f;
        blue = (hex & 0xFF) / 255.0f;
    }

    public Colour getNext() {
        return VALUES[(ordinal() + 1) % 16];
    }

    public Colour getPrevious() {
        return VALUES[(ordinal() + 15) % 16];
    }

    public int getHex() {
        return hex;
    }

    public float getR() {
        return red;
    }

    public float getG() {
        return green;
    }

    public float getB() {
        return blue;
    }
}
