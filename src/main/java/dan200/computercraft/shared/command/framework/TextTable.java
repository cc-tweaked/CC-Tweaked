package dan200.computercraft.shared.command.framework;

import com.google.common.collect.Lists;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.ComputerCraftPacket;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static dan200.computercraft.shared.command.framework.ChatHelpers.coloured;
import static dan200.computercraft.shared.command.framework.ChatHelpers.text;
import static dan200.computercraft.shared.command.framework.TextFormatter.*;

public class TextTable
{
    private static final ITextComponent SEPARATOR = coloured( "| ", TextFormatting.GRAY );
    private static final ITextComponent LINE = text( "\n" );

    private final int id;
    private int columns = -1;
    private final ITextComponent[] header;
    private final List<ITextComponent[]> rows = Lists.newArrayList();

    public TextTable( int id, @Nonnull ITextComponent... header )
    {
        this.id = id;
        this.header = header;
        this.columns = header.length;
    }

    public TextTable( int id )
    {
        this.id = id;
        this.header = null;
    }

    public TextTable( int id, @Nonnull String... header )
    {
        this.id = id;
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

        // Limit the number of rows to fit within a single chat window on default Minecraft
        // options.
        int height = isPlayer( sender ) ? 18 : 100;
        int limit = rows.size() <= height ? rows.size() : height - 1;

        for( int y = 0; y < limit; y++ )
        {
            ITextComponent[] row = rows.get( y );
            for( int i = 0; i < row.length; i++ )
            {
                int width = getWidthFor( row[i], sender );
                if( width > maxWidths[i] ) maxWidths[i] = width;
            }
        }

        // Add a small amount of extra padding. This defaults to 3 spaces for players
        // and 1 for everyone else.
        int padding = isPlayer( sender ) ? getWidth( ' ' ) * 3 : 1;
        for( int i = 0; i < maxWidths.length; i++ ) maxWidths[i] += padding;

        int totalWidth = (columns - 1) * getWidthFor( SEPARATOR, sender );
        for( int x : maxWidths ) totalWidth += x;

        // TODO: Limit the widths of some entries if totalWidth > maxWidth

        List<ITextComponent> out = new ArrayList<>();

        if( header != null )
        {
            TextComponentString line = new TextComponentString( "" );
            for( int i = 0; i < columns - 1; i++ )
            {
                appendFixedWidth( line, sender, header[i], maxWidths[i] );
                line.appendSibling( SEPARATOR );
            }
            line.appendSibling( header[columns - 1] );
            out.add( line );

            // Round the width up rather than down
            int rowCharWidth = getWidthFor( '=', sender );
            int rowWidth = totalWidth / rowCharWidth + (totalWidth % rowCharWidth == 0 ? 0 : 1);
            out.add( coloured( StringUtils.repeat( '=', rowWidth ), TextFormatting.GRAY ) );
        }

        for( int i = 0; i < limit; i++ )
        {
            TextComponentString line = new TextComponentString( "" );
            ITextComponent[] row = rows.get( i );
            for( int j = 0; j < columns - 1; j++ )
            {
                appendFixedWidth( line, sender, row[j], maxWidths[j] );
                line.appendSibling( SEPARATOR );
            }
            line.appendSibling( row[columns - 1] );
            out.add( line );
        }

        if( rows.size() > limit )
        {
            out.add( coloured( (rows.size() - limit) + " additional rows...", TextFormatting.AQUA ) );
        }

        if( isPlayer( sender ) && id != 0 )
        {
            ComputerCraftPacket packet = new ComputerCraftPacket();
            packet.m_packetType = ComputerCraftPacket.PostChat;
            packet.m_dataInt = new int[] { id };

            String[] lines = packet.m_dataString = new String[out.size()];
            for( int i = 0; i < out.size(); i++ )
            {
                lines[i] = ITextComponent.Serializer.componentToJson( out.get( i ) );
            }

            ComputerCraft.sendToPlayer( (EntityPlayerMP) sender, packet );
        }
        else
        {
            ITextComponent result = new TextComponentString( "" );
            for( int i = 0; i < out.size(); i++ )
            {
                if( i > 0 ) result.appendSibling( LINE );
                result.appendSibling( out.get( i ) );
            }
            sender.sendMessage( result );
        }
    }
}
