/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;

/**
 * A reimplementation of the colour system in {@link ArmorDyeRecipe}, but
 * bundled together as an object.
 */
public class ColourTracker
{
    private int total;
    private int totalR;
    private int totalG;
    private int totalB;
    private int count;

    public void addColour( int r, int g, int b )
    {
        total += Math.max( r, Math.max( g, b ) );
        totalR += r;
        totalG += g;
        totalB += b;
        count++;
    }

    public void addColour( float r, float g, float b )
    {
        addColour( (int) (r * 255), (int) (g * 255), (int) (b * 255) );
    }

    public void addColour( DyeColor dye )
    {
        Colour colour = Colour.VALUES[15 - dye.getId()];
        addColour( colour.getR(), colour.getG(), colour.getB() );
    }

    public boolean hasColour()
    {
        return count > 0;
    }

    public int getColour()
    {
        int avgR = totalR / count;
        int avgG = totalG / count;
        int avgB = totalB / count;

        float avgTotal = (float) total / count;
        float avgMax = Math.max( avgR, Math.max( avgG, avgB ) );
        avgR = (int) (avgR * avgTotal / avgMax);
        avgG = (int) (avgG * avgTotal / avgMax);
        avgB = (int) (avgB * avgTotal / avgMax);

        return (avgR << 16) | (avgG << 8) | avgB;
    }
}
