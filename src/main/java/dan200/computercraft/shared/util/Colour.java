/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

public enum Colour
{
    Black( 0x111111 ),
    Red( 0xcc4c4c ),
    Green( 0x57A64E ),
    Brown( 0x7f664c ),
    Blue( 0x3366cc ),
    Purple( 0xb266e5 ),
    Cyan( 0x4c99b2 ),
    LightGrey( 0x999999 ),
    Grey( 0x4c4c4c ),
    Pink( 0xf2b2cc ),
    Lime( 0x7fcc19 ),
    Yellow( 0xdede6c ),
    LightBlue( 0x99b2f2 ),
    Magenta( 0xe57fd8 ),
    Orange( 0xf2b233 ),
    White( 0xf0f0f0 );

    public static final Colour[] VALUES = values();

    public static Colour fromInt( int colour )
    {
        if( colour >= 0 && colour < 16 )
        {
            return Colour.VALUES[colour];
        }
        return null;
    }

    public static Colour fromHex( int colour )
    {
        for( Colour entry : VALUES )
        {
            if( entry.getHex() == colour ) return entry;
        }

        return null;
    }

    private int m_hex;
    private float[] m_rgb;

    Colour( int hex )
    {
        m_hex = hex;
        m_rgb = new float[] {
            ((hex >> 16) & 0xFF) / 255.0f,
            ((hex >> 8) & 0xFF) / 255.0f,
            ((hex) & 0xFF) / 255.0f,
        };
    }

    public Colour getNext()
    {
        return Colour.VALUES[(ordinal() + 1) % 16];
    }

    public Colour getPrevious()
    {
        return Colour.VALUES[(ordinal() + 15) % 16];
    }

    public int getHex()
    {
        return m_hex;
    }

    public float[] getRGB()
    {
        return m_rgb;
    }

    public float getR()
    {
        return m_rgb[0];
    }

    public float getG()
    {
        return m_rgb[1];
    }

    public float getB()
    {
        return m_rgb[2];
    }
}
