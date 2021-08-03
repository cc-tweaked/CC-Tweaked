/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.upload.FileUpload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class UploadFileMessage extends ComputerServerMessage
{
    public static final int MAX_SIZE = 30 * 1024; // Max packet size is 32767. TODO: Bump this in the future
    private final List<FileUpload> files;

    public UploadFileMessage( int instanceId, List<FileUpload> files )
    {
        super( instanceId );
        this.files = files;
    }

    public UploadFileMessage( @Nonnull FriendlyByteBuf buf )
    {
        super( buf );
        int nFiles = buf.readVarInt();
        List<FileUpload> files = this.files = new ArrayList<>( nFiles );
        for( int i = 0; i < nFiles; i++ )
        {
            String name = buf.readUtf( 32767 );
            int size = buf.readVarInt();
            if( size > MAX_SIZE ) break;

            ByteBuffer buffer = ByteBuffer.allocateDirect( size );
            buf.readBytes( buffer );
            buffer.flip();

            files.add( new FileUpload( name, buffer ) );
        }
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        super.toBytes( buf );
        buf.writeVarInt( files.size() );
        for( FileUpload file : files )
        {
            buf.writeUtf( file.getName() );
            buf.writeVarInt( file.getBytes().remaining() );
            buf.writeBytes( file.getBytes() );
        }
    }

    @Override
    protected void handle( NetworkEvent.Context context, @Nonnull ServerComputer computer, @Nonnull IContainerComputer container )
    {
        ServerPlayer player = context.getSender();
        if( player != null ) container.upload( player, files );
    }
}
