/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import static dan200.computercraft.shared.command.text.ChatHelpers.coloured;
import static dan200.computercraft.shared.command.text.ChatHelpers.translate;

public interface TableFormatter
{
    ITextComponent SEPARATOR = coloured( "| ", TextFormatting.GRAY );
    ITextComponent HEADER = coloured( "=", TextFormatting.GRAY );

    /**
     * Get additional padding for the component
     *
     * @param component The component to pad
     * @param width     The desired width for the component
     * @return The padding for this component, or {@code null} if none is needed.
     */
    @Nullable
    ITextComponent getPadding( ITextComponent component, int width );

    /**
     * Get the minimum padding between each column
     *
     * @return The minimum padding.
     */
    int getColumnPadding();

    int getWidth( ITextComponent component );

    void writeLine( int id, ITextComponent component );

    default int display( TableBuilder table )
    {
        if( table.getColumns() <= 0 ) return 0;

        int rowId = table.getId();
        int columns = table.getColumns();
        int[] maxWidths = new int[columns];

        ITextComponent[] headers = table.getHeaders();
        if( headers != null )
        {
            for( int i = 0; i < columns; i++ ) maxWidths[i] = getWidth( headers[i] );
        }

        for( ITextComponent[] row : table.getRows() )
        {
            for( int i = 0; i < row.length; i++ )
            {
                int width = getWidth( row[i] );
                if( width > maxWidths[i] ) maxWidths[i] = width;
            }
        }

        // Add a small amount of padding after each column
        {
            int padding = getColumnPadding();
            for( int i = 0; i < maxWidths.length - 1; i++ ) maxWidths[i] += padding;
        }

        // And comput the total width
        int totalWidth = (columns - 1) * getWidth( SEPARATOR );
        for( int x : maxWidths ) totalWidth += x;

        if( headers != null )
        {
            TextComponentString line = new TextComponentString( "" );
            for( int i = 0; i < columns - 1; i++ )
            {
                line.appendSibling( headers[i] );
                ITextComponent padding = getPadding( headers[i], maxWidths[i] );
                if( padding != null ) line.appendSibling( padding );
                line.appendSibling( SEPARATOR );
            }
            line.appendSibling( headers[columns - 1] );

            writeLine( rowId++, line );

            // Write a separator line. We round the width up rather than down to make
            // it a tad prettier.
            int rowCharWidth = getWidth( HEADER );
            int rowWidth = totalWidth / rowCharWidth + (totalWidth % rowCharWidth == 0 ? 0 : 1);
            writeLine( rowId++, coloured( StringUtils.repeat( HEADER.getUnformattedText(), rowWidth ), TextFormatting.GRAY ) );
        }

        for( ITextComponent[] row : table.getRows() )
        {
            TextComponentString line = new TextComponentString( "" );
            for( int i = 0; i < columns - 1; i++ )
            {
                line.appendSibling( row[i] );
                ITextComponent padding = getPadding( row[i], maxWidths[i] );
                if( padding != null ) line.appendSibling( padding );
                line.appendSibling( SEPARATOR );
            }
            line.appendSibling( row[columns - 1] );
            writeLine( rowId++, line );
        }

        if( table.getAdditional() > 0 )
        {
            writeLine( rowId++, coloured( translate( "commands.computercraft.generic.additional_rows", table.getAdditional() ), TextFormatting.AQUA ) );
        }

        return rowId - table.getId();
    }
}

