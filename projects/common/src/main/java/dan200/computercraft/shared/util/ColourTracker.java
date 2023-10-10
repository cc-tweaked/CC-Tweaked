// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import dan200.computercraft.core.util.Colour;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;

/**
 * A reimplementation of the colour system in {@link ArmorDyeRecipe}, but
 * bundled together as an object.
 */
public class ColourTracker {
    private int total;
    private int totalR;
    private int totalG;
    private int totalB;
    private int count;

    public void addColour(int r, int g, int b) {
        total += Math.max(r, Math.max(g, b));
        totalR += r;
        totalG += g;
        totalB += b;
        count++;
    }

    public void addColour(float r, float g, float b) {
        addColour((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    public void addColour(DyeColor dye) {
        var colour = Colour.VALUES[15 - dye.getId()];
        addColour(colour.getR(), colour.getG(), colour.getB());
    }

    public boolean hasColour() {
        return count > 0;
    }

    public int getColour() {
        var avgR = totalR / count;
        var avgG = totalG / count;
        var avgB = totalB / count;

        var avgTotal = (float) total / count;
        float avgMax = Math.max(avgR, Math.max(avgG, avgB));
        avgR = (int) (avgR * avgTotal / avgMax);
        avgG = (int) (avgG * avgTotal / avgMax);
        avgB = (int) (avgB * avgTotal / avgMax);

        return (avgR << 16) | (avgG << 8) | avgB;
    }
}
