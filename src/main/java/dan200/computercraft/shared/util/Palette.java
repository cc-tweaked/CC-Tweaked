/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.nbt.CompoundTag;

public class Palette {
    public static final Palette DEFAULT = new Palette();
    private static final int PALETTE_SIZE = 16;
    private final double[][] colours = new double[PALETTE_SIZE][3];

    public Palette() {
        // Get the default palette
        this.resetColours();
    }

    public void resetColours() {
        for (int i = 0; i < Colour.values().length; i++) {
            this.resetColour(i);
        }
    }

    public void resetColour(int i) {
        if (i >= 0 && i < this.colours.length) {
            this.setColour(i, Colour.values()[i]);
        }
    }

    public void setColour(int i, Colour colour) {
        this.setColour(i, colour.getR(), colour.getG(), colour.getB());
    }

    public void setColour(int i, double r, double g, double b) {
        if (i >= 0 && i < this.colours.length) {
            this.colours[i][0] = r;
            this.colours[i][1] = g;
            this.colours[i][2] = b;
        }
    }

    public double[] getColour(int i) {
        if (i >= 0 && i < this.colours.length) {
            return this.colours[i];
        }
        return null;
    }

    public CompoundTag writeToNBT(CompoundTag nbt) {
        int[] rgb8 = new int[this.colours.length];

        for (int i = 0; i < this.colours.length; i++) {
            rgb8[i] = encodeRGB8(this.colours[i]);
        }

        nbt.putIntArray("term_palette", rgb8);
        return nbt;
    }

    public static int encodeRGB8(double[] rgb) {
        int r = (int) (rgb[0] * 255) & 0xFF;
        int g = (int) (rgb[1] * 255) & 0xFF;
        int b = (int) (rgb[2] * 255) & 0xFF;

        return (r << 16) | (g << 8) | b;
    }

    public void readFromNBT(CompoundTag nbt) {
        if (!nbt.contains("term_palette")) {
            return;
        }
        int[] rgb8 = nbt.getIntArray("term_palette");

        if (rgb8.length != this.colours.length) {
            return;
        }

        for (int i = 0; i < this.colours.length; i++) {
            this.colours[i] = decodeRGB8(rgb8[i]);
        }
    }

    public static double[] decodeRGB8(int rgb) {
        return new double[] {
            ((rgb >> 16) & 0xFF) / 255.0f,
            ((rgb >> 8) & 0xFF) / 255.0f,
            (rgb & 0xFF) / 255.0f
        };
    }
}
