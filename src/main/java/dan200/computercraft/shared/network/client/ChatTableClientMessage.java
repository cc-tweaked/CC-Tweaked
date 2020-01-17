/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.ClientTableFormatter;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class ChatTableClientMessage implements NetworkMessage
{
    private TableBuilder table;

    public ChatTableClientMessage( TableBuilder table )
    {
        if( table.getColumns() < 0 ) throw new IllegalStateException( "Cannot send an empty table" );
        this.table = table;
    }

    public ChatTableClientMessage()
    {
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeVarInt( table.getId() );
        buf.writeVarInt( table.getColumns() );
        buf.writeBoolean( table.getHeaders() != null );
        if( table.getHeaders() != null )
        {
            for( ITextComponent header : table.getHeaders() ) buf.writeTextComponent( header );
        }

        buf.writeVarInt( table.getRows().size() );
        for( ITextComponent[] row : table.getRows() )
        {
            for( ITextComponent column : row ) buf.writeTextComponent( column );
        }

        buf.writeVarInt( table.getAdditional() );
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        int id = buf.readVarInt();
        int columns = buf.readVarInt();
        TableBuilder table;
        if( buf.readBoolean() )
        {
            ITextComponent[] headers = new ITextComponent[columns];
            for( int i = 0; i < columns; i++ ) headers[i] = NBTUtil.readTextComponent( buf );
            table = new TableBuilder( id, headers );
        }
        else
        {
            table = new TableBuilder( id );
        }

        int rows = buf.readVarInt();
        for( int i = 0; i < rows; i++ )
        {
            ITextComponent[] row = new ITextComponent[columns];
            for( int j = 0; j < columns; j++ ) row[j] = NBTUtil.readTextComponent( buf );
            table.row( row );
        }

        table.setAdditional( buf.readVarInt() );
        this.table = table;
    }

    @Override
    @SideOnly( Side.CLIENT )
    public void handle( MessageContext context )
    {
        ClientTableFormatter.INSTANCE.display( table );
    }
}
