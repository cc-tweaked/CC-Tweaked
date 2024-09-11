// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.terminal;

import dan200.computercraft.core.util.Colour;


public class Palette {
    public static final int PALETTE_SIZE = 16;

    private final boolean colour;
    private final double[][] colours = new double[PALETTE_SIZE][3];
    private final int[] byteColours = new int[PALETTE_SIZE];

    public static final Palette DEFAULT = new Palette(true);

    public Palette(boolean colour) {
        this.colour = colour;
        resetColours();
    }

    public void setColour(int i, double r, double g, double b) {
        if (i < 0 || i >= PALETTE_SIZE) return;
        colours[i][0] = r;
        colours[i][1] = g;
        colours[i][2] = b;

        if (colour) {
            byteColours[i] = packColour((int) (r * 255), (int) (g * 255), (int) (b * 255));
        } else {
            var grey = (int) ((r + g + b) / 3 * 255);
            byteColours[i] = packColour(grey, grey, grey);
        }
    }

    private static int packColour(int r, int g, int b) {
        return 255 << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
    }

    public void setColour(int i, Colour colour) {
        setColour(i, colour.getR(), colour.getG(), colour.getB());
    }

    public double[] getColour(int i) {
        return colours[i];
    }

    /**
     * Get the colour as a set of ARGB values suitable for rendering. Colours are automatically converted to greyscale
     * when using a black and white palette.
     * <p>
     * This returns a packed 32-bit ARGB colour.
     *
     * @param i The colour index.
     * @return The actual RGB colour.
     */
    public int getRenderColours(int i) {
        return byteColours[i];
    }

    public void resetColours() {
        for (var i = 0; i < Colour.VALUES.length; i++) setColour(i, Colour.VALUES[i]);
    }

    public static int encodeRGB8(double[] rgb) {
        var r = (int) (rgb[0] * 255) & 0xFF;
        var g = (int) (rgb[1] * 255) & 0xFF;
        var b = (int) (rgb[2] * 255) & 0xFF;

        return (r << 16) | (g << 8) | b;
    }

    public static double[] decodeRGB8(int rgb) {
        return new double[]{
            ((rgb >> 16) & 0xFF) / 255.0f,
            ((rgb >> 8) & 0xFF) / 255.0f,
            (rgb & 0xFF) / 255.0f,
        };
    }
}
