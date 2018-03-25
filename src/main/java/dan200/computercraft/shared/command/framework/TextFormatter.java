package dan200.computercraft.shared.command.framework;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.FakePlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import static dan200.computercraft.shared.command.framework.ChatHelpers.coloured;

/**
 * Adapated from Sponge's PaginationCalculator
 */
public class TextFormatter
{
    private static final int SPACE_WIDTH = 4;
    private static final char PADDING_CHAR = 'ˌ';

    /**
     * Yoinked from FontRenderer
     *
     * @see net.minecraft.client.gui.FontRenderer#charWidth
     * @see net.minecraft.client.gui.FontRenderer#getCharWidth(char)
     */
    private static final String CHARACTERS = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";
    private static final int[] CHAR_WIDTHS = new int[]{
        6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 6, 6, 4,
        4, 6, 7, 6, 6, 6, 6, 6, 6, 1, 1, 1, 1, 1, 1, 1,
        1, 2, 5, 6, 6, 6, 6, 3, 5, 5, 5, 6, 2, 6, 2, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 2, 2, 5, 6, 5, 6,
        7, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 4, 6, 6,
        3, 6, 6, 6, 6, 6, 5, 6, 6, 2, 6, 5, 3, 6, 6, 6,
        6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 5, 2, 5, 7, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 3, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6,
        6, 3, 6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6, 2, 6, 6,
        8, 9, 9, 6, 6, 6, 8, 8, 6, 8, 8, 8, 8, 8, 6, 6,
        9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
        9, 9, 9, 9, 9, 9, 9, 9, 9, 6, 9, 9, 9, 5, 9, 9,
        8, 7, 7, 8, 7, 8, 8, 8, 7, 8, 8, 7, 9, 9, 6, 7,
        7, 7, 7, 7, 9, 6, 7, 8, 7, 6, 6, 9, 7, 6, 7, 1
    };

    private static final int[] EXTRA_CHARS = new int[]{
        '\u20e2', '\u261b',
    };

    private static final byte[] EXTRA_WIDTHS = new byte[]{
        8, 4,
    };

    private static int getWidth( int codePoint )
    {
        // Escape codes
        if( codePoint == 167 ) return -1;

        // Space and non-breaking space
        if( codePoint == 32 || codePoint == 160 ) return 4;

        // Built-in characters
        int nonUnicodeIdx = CHARACTERS.indexOf( codePoint );
        if( codePoint > 0 && nonUnicodeIdx != -1 ) return CHAR_WIDTHS[nonUnicodeIdx];

        // Other special characters we use.
        int extraIdx = Arrays.binarySearch( EXTRA_CHARS, codePoint );
        if( extraIdx >= 0 ) return EXTRA_WIDTHS[extraIdx];

        return 0;
    }

    private static int getWidth( ITextComponent component )
    {
        int total = 0;
        if( component instanceof TextComponentString )
        {
            String contents = component.getUnformattedComponentText();

            int bold = component.getStyle().getBold() ? 1 : 0;
            for( int i = 0; i < contents.length(); i++ )
            {
                int cp = contents.charAt( i );
                assert cp != '\n';
                int width = getWidth( cp );
                if( width < 0 )
                {
                    i++;
                }
                else
                {
                    total += width + bold;
                }
            }
        }

        for( ITextComponent child : component.getSiblings() )
        {
            total += getWidth( child );
        }

        return total;
    }

    public static boolean isPlayer( ICommandSender sender )
    {
        return sender instanceof EntityPlayerMP && !(sender instanceof FakePlayer);
    }

    public static int getMaxWidth( ICommandSender sender )
    {
        return isPlayer( sender ) ? 320 : 80;
    }

    public static int getWidthFor( ITextComponent component, ICommandSender sender )
    {
        return isPlayer( sender ) ? getWidth( component ) : component.getUnformattedText().length();
    }

    public static int getWidthFor( int codepoint, ICommandSender sender )
    {
        return isPlayer( sender ) ? getWidth( codepoint ) : 1;
    }

    public static void appendFixedWidth( ITextComponent out, ICommandSender sender, ITextComponent entry, int maxWidth )
    {
        out.appendSibling( entry );

        int width = getWidthFor( entry, sender );
        int delta = maxWidth - width;

        if( delta > 0 )
        {
            int spaces = delta / SPACE_WIDTH;
            int extra = delta % SPACE_WIDTH;

            // Append a fixed number of spaces
            if( spaces > 0 ) out.appendSibling( new TextComponentString( StringUtils.repeat( ' ', spaces ) ) );

            // Append several minor characters to pad to a full string 
            if( extra > 0 )
            {
                out.appendSibling( coloured( StringUtils.repeat( PADDING_CHAR, extra ), TextFormatting.GRAY ) );
            }
        }
    }
}
