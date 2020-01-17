/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.util.text.TextComponentTranslation;

public final class StringUtil
{
    private StringUtil() {}

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

    /**
     * Translates a string.
     *
     * Try to avoid using this where possible - it is generally preferred to use {@link TextComponentTranslation}.
     *
     * @param key The key to translate.
     * @return The translated string.
     */
    @SuppressWarnings( "deprecation" )
    public static String translate( String key )
    {
        return net.minecraft.util.text.translation.I18n.translateToLocal( key );
    }

    /**
     * Translates and formats a string.
     *
     * Try to avoid using this where possible - it is generally preferred to use {@link TextComponentTranslation}.
     *
     * @param key  The key to translate.
     * @param args The arguments to supply to {@link String#format(String, Object...)}.
     * @return The translated and formatted string.
     */
    @SuppressWarnings( "deprecation" )
    public static String translateFormatted( String key, Object... args )
    {
        return net.minecraft.util.text.translation.I18n.translateToLocalFormatted( key, args );
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
