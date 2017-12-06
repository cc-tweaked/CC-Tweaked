package dan200.computercraft.shared.command.framework;

import com.google.common.collect.Lists;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.FakePlayer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;

import static dan200.computercraft.shared.command.framework.ChatHelpers.coloured;
import static dan200.computercraft.shared.command.framework.ChatHelpers.text;

public class TextTable
{
    private static final String CHARACTERS = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";
    private static final int[] CHAR_WIDTHS = new int[] {
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

    private static final ITextComponent SEPARATOR = coloured( " | ", TextFormatting.GRAY );
    private static final ITextComponent LINE = text( "\n" );

    private static int getWidth( char character, ICommandSender sender )
    {
        if( sender instanceof EntityPlayerMP && !(sender instanceof FakePlayer) )
        {
            // Use font widths here.
            if( character == 167 )
            {
                return -1;
            }
            else if( character == 32 )
            {
                return 4;
            }
            else if( CHARACTERS.indexOf( character ) != -1 )
            {
                return CHAR_WIDTHS[ character ];
            }
            else
            {
                // Eh, close enough.
                return 6;
            }
        }
        else
        {
            return 1;
        }
    }

    private static int getWidth( ITextComponent text, ICommandSender sender )
    {
        int sum = 0;
        String chars = text.getUnformattedText();
        for( int i = 0; i < chars.length(); i++ )
        {
            sum += getWidth( chars.charAt( i ), sender );
        }

        return sum;
    }

    private static boolean isPlayer( ICommandSender sender )
    {
        return sender instanceof EntityPlayerMP && !(sender instanceof FakePlayer);
    }

    private static int getMaxWidth( ICommandSender sender )
    {
        return isPlayer( sender ) ? 320 : 80;
    }

    private int columns = -1;
    private final ITextComponent[] header;
    private final List<ITextComponent[]> rows = Lists.newArrayList();

    public TextTable( @Nonnull ITextComponent... header )
    {
        this.header = header;
        this.columns = header.length;
    }

    public TextTable()
    {
        header = null;
    }

    public TextTable( @Nonnull String... header )
    {
        this.header = new ITextComponent[ header.length ];
        for( int i = 0; i < header.length; i++ )
        {
            this.header[ i ] = ChatHelpers.header( header[ i ] );
        }
        this.columns = header.length;
    }

    public void addRow( @Nonnull ITextComponent... row )
    {
        if( columns == -1 )
        {
            columns = row.length;
        }
        else if( row.length != columns )
        {
            throw new IllegalArgumentException( "Row is the incorrect length" );
        }

        rows.add( row );
    }

    public void displayTo( ICommandSender sender )
    {
        if( columns <= 0 ) return;

        final int maxWidth = getMaxWidth( sender );

        int[] minWidths = new int[ columns ];
        int[] maxWidths = new int[ columns ];
        int[] rowWidths = new int[ columns ];

        if( header != null )
        {
            for( int i = 0; i < columns; i++ )
            {
                maxWidths[ i ] = minWidths[ i ] = getWidth( header[ i ], sender );
            }
        }

        for( ITextComponent[] row : rows )
        {
            for( int i = 0; i < row.length; i++ )
            {
                int width = getWidth( row[ i ], sender );
                rowWidths[ i ] += width;
                if( width > maxWidths[ i ] )
                {
                    maxWidths[ i ] = width;
                }
            }
        }

        // Calculate the average width
        for( int i = 0; i < columns; i++ )
        {
            rowWidths[ i ] = Math.max( rowWidths[ i ], rows.size() );
        }

        int totalWidth = (columns - 1) * getWidth( SEPARATOR, sender );
        for( int x : maxWidths ) totalWidth += x;

        // TODO: Limit the widths of some entries if totalWidth > maxWidth

        ITextComponent out = new TextComponentString( "" );

        if( header != null )
        {
            for( int i = 0; i < columns; i++ )
            {
                if( i != 0 ) out.appendSibling( SEPARATOR );
                appendFixed( out, sender, header[ i ], maxWidths[ i ] );
            }
            out.appendSibling( LINE );

            // Round the width up rather than down
            int rowCharWidth = getWidth( '=', sender );
            int rowWidth = totalWidth / rowCharWidth + (totalWidth % rowCharWidth == 0 ? 0 : 1);
            out.appendSibling( coloured( StringUtils.repeat( '=', rowWidth ), TextFormatting.GRAY ) );
            out.appendSibling( LINE );
        }

        for( int i = 0; i < rows.size(); i++ )
        {
            ITextComponent[] row = rows.get( i );
            if( i != 0 ) out.appendSibling( LINE );
            for( int j = 0; j < columns; j++ )
            {
                if( j != 0 ) out.appendSibling( SEPARATOR );
                appendFixed( out, sender, row[ j ], maxWidths[ j ] );
            }
        }

        sender.sendMessage( out );
    }

    private static void appendFixed( ITextComponent out, ICommandSender sender, ITextComponent entry, int maxWidth )
    {
        int length = getWidth( entry, sender );
        int delta = length - maxWidth;
        if( delta < 0 )
        {
            // Convert to overflow;
            delta = -delta;

            // We have to remove some padding as there is a padding added between formatted and unformatted text
            if( !entry.getStyle().isEmpty() && isPlayer( sender ) ) delta -= 1;

            out.appendSibling( entry );

            int spaceWidth = getWidth( ' ', sender );

            int spaces = delta / spaceWidth;
            int missing = delta % spaceWidth;
            spaces -= missing;

            ITextComponent component = new TextComponentString( StringUtils.repeat( ' ', spaces < 0 ? 0 : spaces ) );
            if( missing > 0 )
            {
                ITextComponent bold = new TextComponentString( StringUtils.repeat( ' ', missing ) );
                bold.getStyle().setBold( true );
                component.appendSibling( bold );
            }

            out.appendSibling( component );
        }
        else if( delta > 0 )
        {
            out.appendSibling( entry );
        }
        else
        {
            out.appendSibling( entry );

            // We have to add some padding as we expect a padding between formatted and unformatted text
            // and there won't be.
            if( entry.getStyle().isEmpty() && isPlayer( sender ) ) out.appendText( " " );
        }
    }
}
