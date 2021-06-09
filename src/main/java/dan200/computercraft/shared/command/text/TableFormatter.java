/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import static dan200.computercraft.shared.command.text.ChatHelpers.coloured;
import static dan200.computercraft.shared.command.text.ChatHelpers.translate;

public interface TableFormatter
{
    Text SEPARATOR = coloured( "| ", Formatting.GRAY );
    Text HEADER = coloured( "=", Formatting.GRAY );

    default int display( TableBuilder table )
    {
        if( table.getColumns() <= 0 )
        {
            return 0;
        }

        int rowId = table.getId();
        int columns = table.getColumns();
        int[] maxWidths = new int[columns];

        Text[] headers = table.getHeaders();
        if( headers != null )
        {
            for( int i = 0; i < columns; i++ )
            {
                maxWidths[i] = this.getWidth( headers[i] );
            }
        }

        for( Text[] row : table.getRows() )
        {
            for( int i = 0; i < row.length; i++ )
            {
                int width = this.getWidth( row[i] );
                if( width > maxWidths[i] )
                {
                    maxWidths[i] = width;
                }
            }
        }

        // Add a small amount of padding after each column
        {
            int padding = this.getColumnPadding();
            for( int i = 0; i < maxWidths.length - 1; i++ )
            {
                maxWidths[i] += padding;
            }
        }

        // And compute the total width
        int totalWidth = (columns - 1) * this.getWidth( SEPARATOR );
        for( int x : maxWidths )
        {
            totalWidth += x;
        }

        if( headers != null )
        {
            LiteralText line = new LiteralText( "" );
            for( int i = 0; i < columns - 1; i++ )
            {
                line.append( headers[i] );
                Text padding = this.getPadding( headers[i], maxWidths[i] );
                if( padding != null )
                {
                    line.append( padding );
                }
                line.append( SEPARATOR );
            }
            line.append( headers[columns - 1] );

            this.writeLine( rowId++, line );

            // Write a separator line. We round the width up rather than down to make
            // it a tad prettier.
            int rowCharWidth = this.getWidth( HEADER );
            int rowWidth = totalWidth / rowCharWidth + (totalWidth % rowCharWidth == 0 ? 0 : 1);
            this.writeLine( rowId++, coloured( StringUtils.repeat( HEADER.getString(), rowWidth ), Formatting.GRAY ) );
        }

        for( Text[] row : table.getRows() )
        {
            LiteralText line = new LiteralText( "" );
            for( int i = 0; i < columns - 1; i++ )
            {
                line.append( row[i] );
                Text padding = this.getPadding( row[i], maxWidths[i] );
                if( padding != null )
                {
                    line.append( padding );
                }
                line.append( SEPARATOR );
            }
            line.append( row[columns - 1] );
            this.writeLine( rowId++, line );
        }

        if( table.getAdditional() > 0 )
        {
            this.writeLine( rowId++, coloured( translate( "commands.computercraft.generic.additional_rows", table.getAdditional() ), Formatting.AQUA ) );
        }

        return rowId - table.getId();
    }

    int getWidth( Text component );

    /**
     * Get the minimum padding between each column.
     *
     * @return The minimum padding.
     */
    int getColumnPadding();

    /**
     * Get additional padding for the component.
     *
     * @param component The component to pad
     * @param width     The desired width for the component
     * @return The padding for this component, or {@code null} if none is needed.
     */
    @Nullable
    Text getPadding( Text component, int width );

    void writeLine( int id, Text component );
}
