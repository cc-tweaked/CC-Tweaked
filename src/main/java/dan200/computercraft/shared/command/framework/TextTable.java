package dan200.computercraft.shared.command.framework;

import com.google.common.collect.Lists;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;

import static dan200.computercraft.shared.command.framework.ChatHelpers.coloured;
import static dan200.computercraft.shared.command.framework.ChatHelpers.text;
import static dan200.computercraft.shared.command.framework.TextFormatter.*;

public class TextTable
{
    private static final ITextComponent SEPARATOR = coloured( "| ", TextFormatting.GRAY );
    private static final ITextComponent LINE = text( "\n" );

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
        this.header = new ITextComponent[header.length];
        for( int i = 0; i < header.length; i++ )
        {
            this.header[i] = ChatHelpers.header( header[i] );
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

        int[] maxWidths = new int[columns];

        if( header != null )
        {
            for( int i = 0; i < columns; i++ )
            {
                maxWidths[i] = getWidthFor( header[i], sender );
            }
        }

        // Limit the number of rows to something sensible.
        int limit = isPlayer( sender ) ? 30 : 100;
        if( limit > rows.size() ) limit = rows.size();

        for( int y = 0; y < limit; y++ )
        {
            ITextComponent[] row = rows.get( y );
            for( int i = 0; i < row.length; i++ )
            {
                int width = getWidthFor( row[i], sender ) + 3;
                if( width > maxWidths[i] ) maxWidths[i] = width;
            }
        }

        // Add a small amount of extra padding. We include this here instead of the separator to allow
        // for "extra" characters
        for( int i = 0; i < maxWidths.length; i++ ) maxWidths[i] += 4;

        int totalWidth = (columns - 1) * getWidthFor( SEPARATOR, sender );
        for( int x : maxWidths ) totalWidth += x;

        // TODO: Limit the widths of some entries if totalWidth > maxWidth

        ITextComponent out = new TextComponentString( "" );

        if( header != null )
        {
            for( int i = 0; i < columns - 1; i++ )
            {
                appendFixedWidth( out, sender, header[i], maxWidths[i] );
                out.appendSibling( SEPARATOR );
            }
            out.appendSibling( header[columns - 1] );
            out.appendSibling( LINE );

            // Round the width up rather than down
            int rowCharWidth = getWidthFor( '=', sender );
            int rowWidth = totalWidth / rowCharWidth + (totalWidth % rowCharWidth == 0 ? 0 : 1);
            out.appendSibling( coloured( StringUtils.repeat( '=', rowWidth ), TextFormatting.GRAY ) );
            out.appendSibling( LINE );
        }

        for( int i = 0; i < limit; i++ )
        {
            ITextComponent[] row = rows.get( i );
            if( i != 0 ) out.appendSibling( LINE );
            for( int j = 0; j < columns - 1; j++ )
            {
                appendFixedWidth( out, sender, row[j], maxWidths[j] );
                out.appendSibling( SEPARATOR );
            }
            out.appendSibling( row[columns - 1] );
        }

        if( limit != rows.size() )
        {
            out.appendSibling( LINE );
            out.appendSibling( coloured( (rows.size() - limit) + " additional rows...", TextFormatting.AQUA ) );
        }

        sender.sendMessage( out );
    }
}
