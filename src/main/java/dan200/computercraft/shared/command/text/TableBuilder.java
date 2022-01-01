/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.text;

import dan200.computercraft.shared.command.CommandUtils;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.ChatTableClientMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TableBuilder
{
    private final int id;
    private int columns = -1;
    private final Component[] headers;
    private final ArrayList<Component[]> rows = new ArrayList<>();
    private int additional;

    public TableBuilder( int id, @Nonnull Component... headers )
    {
        if( id < 0 ) throw new IllegalArgumentException( "ID must be positive" );
        this.id = id;
        this.headers = headers;
        columns = headers.length;
    }

    public TableBuilder( int id )
    {
        if( id < 0 ) throw new IllegalArgumentException( "ID must be positive" );
        this.id = id;
        headers = null;
    }

    public TableBuilder( int id, @Nonnull String... headers )
    {
        if( id < 0 ) throw new IllegalArgumentException( "ID must be positive" );
        this.id = id;
        this.headers = new Component[headers.length];
        columns = headers.length;

        for( int i = 0; i < headers.length; i++ ) this.headers[i] = ChatHelpers.header( headers[i] );
    }

    public void row( @Nonnull Component... row )
    {
        if( columns == -1 ) columns = row.length;
        if( row.length != columns ) throw new IllegalArgumentException( "Row is the incorrect length" );
        rows.add( row );
    }

    /**
     * Get the unique identifier for this table type.
     *
     * When showing a table within Minecraft, previous instances of this table with
     * the same ID will be removed from chat.
     *
     * @return This table's type.
     */
    public int getId()
    {
        return id;
    }

    /**
     * Get the number of columns for this table.
     *
     * This will be the same as {@link #getHeaders()}'s length if it is is non-{@code null},
     * otherwise the length of the first column.
     *
     * @return The number of columns.
     */
    public int getColumns()
    {
        return columns;
    }

    @Nullable
    public Component[] getHeaders()
    {
        return headers;
    }

    @Nonnull
    public List<Component[]> getRows()
    {
        return rows;
    }

    public int getAdditional()
    {
        return additional;
    }

    public void setAdditional( int additional )
    {
        this.additional = additional;
    }

    /**
     * Trim this table to a given height.
     *
     * @param height The desired height.
     */
    public void trim( int height )
    {
        if( rows.size() > height )
        {
            additional += rows.size() - height - 1;
            rows.subList( height - 1, rows.size() ).clear();
        }
    }

    public void display( CommandSourceStack source )
    {
        if( CommandUtils.isPlayer( source ) )
        {
            trim( 18 );
            NetworkHandler.sendToPlayer( (ServerPlayer) source.getEntity(), new ChatTableClientMessage( this ) );
        }
        else
        {
            trim( 100 );
            new ServerTableFormatter( source ).display( this );
        }
    }
}
