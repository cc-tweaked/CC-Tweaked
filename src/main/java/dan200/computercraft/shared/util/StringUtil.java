/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

public class StringUtil
{
    public static String normaliseLabel( String label )
    {
        if( label == null ) return null;

        int length = Math.min( 32, label.length() );
        StringBuilder builder = new StringBuilder( length );
        for( int i = 0; i < length; i++ )
        {
            char c = label.charAt( i );
            if( (c >= ' ' && c <= '~') || (c >= 161 && c <= 172) || (c >= 174 && c <= 255) )
            {
                builder.append( c );
            }
            else
            {
                builder.append( '?' );
            }
        }

        return builder.toString();
    }

    public static byte[] encodeString( String string )
    {
        byte[] chars = new byte[string.length()];

        for( int i = 0; i < chars.length; i++ )
        {
            char c = string.charAt( i );
            chars[i] = c < 256 ? (byte) c : 63;
        }

        return chars;
    }
}
