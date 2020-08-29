/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

/**
 * A reimplementation of the colour system in {@link net.minecraft.item.crafting.RecipesArmorDyes}, but bundled together as an object.
 */
public class ColourTracker {
    private int total;
    private int totalR;
    private int totalG;
    private int totalB;
    private int count;

    public void addColour(float r, float g, float b) {
        this.addColour((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    public void addColour(int r, int g, int b) {
        this.total += Math.max(r, Math.max(g, b));
        this.totalR += r;
        this.totalG += g;
        this.totalB += b;
        this.count++;
    }

    public boolean hasColour() {
        return this.count > 0;
    }

    public int getColour() {
        int avgR = this.totalR / this.count;
        int avgG = this.totalG / this.count;
        int avgB = this.totalB / this.count;

        float avgTotal = (float) this.total / this.count;
        float avgMax = Math.max(avgR, Math.max(avgG, avgB));
        avgR = (int) (avgR * avgTotal / avgMax);
        avgG = (int) (avgG * avgTotal / avgMax);
        avgB = (int) (avgB * avgTotal / avgMax);

        return (avgR << 16) | (avgG << 8) | avgB;
    }
}
