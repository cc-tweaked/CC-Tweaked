/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.nbt.CompoundNBT;

public class Palette
{
    private static final int PALETTE_SIZE = 16;
    private final double[][] colours = new double[PALETTE_SIZE][3];

    public static final Palette DEFAULT = new Palette();

    public Palette()
    {
        // Get the default palette
        resetColours();
    }

    public void setColour( int i, double r, double g, double b )
    {
        if( i >= 0 && i < colours.length )
        {
            colours[i][0] = r;
            colours[i][1] = g;
            colours[i][2] = b;
        }
    }

    public void setColour( int i, Colour colour )
    {
        setColour( i, colour.getR(), colour.getG(), colour.getB() );
    }

    public double[] getColour( int i )
    {
        if( i >= 0 && i < colours.length )
        {
            return colours[i];
        }
        return null;
    }

    public void resetColour( int i )
    {
        if( i >= 0 && i < colours.length )
        {
            setColour( i, Colour.VALUES[i] );
        }
    }

    public void resetColours()
    {
        for( int i = 0; i < Colour.VALUES.length; i++ )
        {
            resetColour( i );
        }
    }

    public static int encodeRGB8( double[] rgb )
    {
        int r = (int) (rgb[0] * 255) & 0xFF;
        int g = (int) (rgb[1] * 255) & 0xFF;
        int b = (int) (rgb[2] * 255) & 0xFF;

        return (r << 16) | (g << 8) | b;
    }

    public static double[] decodeRGB8( int rgb )
    {
        return new double[] {
            ((rgb >> 16) & 0xFF) / 255.0f,
            ((rgb >> 8) & 0xFF) / 255.0f,
            (rgb & 0xFF) / 255.0f,
        };
    }

    public CompoundNBT writeToNBT( CompoundNBT nbt )
    {
        int[] rgb8 = new int[colours.length];

        for( int i = 0; i < colours.length; i++ )
        {
            rgb8[i] = encodeRGB8( colours[i] );
        }

        nbt.putIntArray( "term_palette", rgb8 );
        return nbt;
    }

    public void readFromNBT( CompoundNBT nbt )
    {
        if( !nbt.contains( "term_palette" ) ) return;
        int[] rgb8 = nbt.getIntArray( "term_palette" );

        if( rgb8.length != colours.length ) return;

        for( int i = 0; i < colours.length; i++ )
        {
            colours[i] = decodeRGB8( rgb8[i] );
        }
    }
}
