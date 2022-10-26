/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;

public class Palette
{
    private static final int PALETTE_SIZE = 16;

    private final boolean colour;
    private final double[][] colours = new double[PALETTE_SIZE][3];
    private final byte[][] byteColours = new byte[PALETTE_SIZE][4];

    public static final Palette DEFAULT = new Palette( true );

    public Palette( boolean colour )
    {
        this.colour = colour;
        resetColours();

        for( int i = 0; i < PALETTE_SIZE; i++ ) byteColours[i][3] = (byte) 255;
    }

    public void setColour( int i, double r, double g, double b )
    {
        if( i < 0 || i >= colours.length ) return;
        colours[i][0] = r;
        colours[i][1] = g;
        colours[i][2] = b;

        if( colour )
        {
            byteColours[i][0] = (byte) (int) (r * 255);
            byteColours[i][1] = (byte) (int) (g * 255);
            byteColours[i][2] = (byte) (int) (b * 255);
        }
        else
        {
            byte grey = (byte) (int) ((r + g + b) / 3 * 255);
            byteColours[i][0] = byteColours[i][1] = byteColours[i][2] = grey;
        }
    }

    public void setColour( int i, Colour colour )
    {
        setColour( i, colour.getR(), colour.getG(), colour.getB() );
    }

    public double[] getColour( int i )
    {
        return i >= 0 && i < colours.length ? colours[i] : null;
    }

    /**
     * Get the colour as a set of RGB values suitable for rendering. Colours are automatically converted to greyscale
     * when using a black and white palette.
     * <p>
     * This returns a byte array, suitable for being used directly by our terminal vertex format.
     *
     * @param i The colour index.
     * @return The number as a tuple of bytes.
     */
    @Nonnull
    public byte[] getRenderColours( int i )
    {
        return byteColours[i];
    }

    public void resetColour( int i )
    {
        if( i >= 0 && i < colours.length ) setColour( i, Colour.VALUES[i] );
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

    public void write( FriendlyByteBuf buffer )
    {
        for( double[] colour : colours )
        {
            for( double channel : colour ) buffer.writeByte( (int) (channel * 0xFF) & 0xFF );
        }
    }

    public void read( FriendlyByteBuf buffer )
    {
        for( int i = 0; i < PALETTE_SIZE; i++ )
        {
            double r = (buffer.readByte() & 0xFF) / 255.0;
            double g = (buffer.readByte() & 0xFF) / 255.0;
            double b = (buffer.readByte() & 0xFF) / 255.0;
            setColour( i, r, g, b );
        }
    }

    public CompoundTag writeToNBT( CompoundTag nbt )
    {
        int[] rgb8 = new int[colours.length];

        for( int i = 0; i < colours.length; i++ )
        {
            rgb8[i] = encodeRGB8( colours[i] );
        }

        nbt.putIntArray( "term_palette", rgb8 );
        return nbt;
    }

    public void readFromNBT( CompoundTag nbt )
    {
        if( !nbt.contains( "term_palette" ) ) return;
        int[] rgb8 = nbt.getIntArray( "term_palette" );

        if( rgb8.length != colours.length ) return;

        for( int i = 0; i < colours.length; i++ )
        {
            double[] colours = decodeRGB8( rgb8[i] );
            setColour( i, colours[0], colours[1], colours[2] );
        }
    }
}
