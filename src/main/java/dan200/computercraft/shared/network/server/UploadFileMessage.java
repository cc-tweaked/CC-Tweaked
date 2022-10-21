/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.menu.ServerInputHandler;
import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.network.NetworkHandler;
import io.netty.handler.codec.DecoderException;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UploadFileMessage extends ComputerServerMessage
{
    public static final int MAX_SIZE = 512 * 1024;
    static final int MAX_PACKET_SIZE = 30 * 1024; // Max packet size is 32767.

    public static final int MAX_FILES = 32;
    public static final int MAX_FILE_NAME = 128;

    private static final int FLAG_FIRST = 1;
    private static final int FLAG_LAST = 2;

    private final UUID uuid;
    private final int flag;
    private final List<FileUpload> files;
    private final List<FileSlice> slices;

    UploadFileMessage( Container menu, UUID uuid, int flag, List<FileUpload> files, List<FileSlice> slices )
    {
        super( menu );
        this.uuid = uuid;
        this.flag = flag;
        this.files = files;
        this.slices = slices;
    }

    public UploadFileMessage( @Nonnull PacketBuffer buf )
    {
        super( buf );
        uuid = buf.readUUID();
        int flag = this.flag = buf.readByte();

        int totalSize = 0;
        if( (flag & FLAG_FIRST) != 0 )
        {
            int nFiles = buf.readVarInt();
            if( nFiles >= MAX_FILES ) throw new DecoderException( "Too many files" );

            List<FileUpload> files = this.files = new ArrayList<>( nFiles );
            for( int i = 0; i < nFiles; i++ )
            {
                String name = buf.readUtf( MAX_FILE_NAME );
                int size = buf.readVarInt();
                if( size > MAX_SIZE || (totalSize += size) >= MAX_SIZE )
                {
                    throw new DecoderException( "Files are too large" );
                }

                byte[] digest = new byte[FileUpload.CHECKSUM_LENGTH];
                buf.readBytes( digest );

                files.add( new FileUpload( name, ByteBuffer.allocateDirect( size ), digest ) );
            }
        }
        else
        {
            files = null;
        }

        int nSlices = buf.readVarInt();
        List<FileSlice> slices = this.slices = new ArrayList<>( nSlices );
        for( int i = 0; i < nSlices; i++ )
        {
            int fileId = buf.readUnsignedByte();
            int offset = buf.readVarInt();

            int size = buf.readUnsignedShort();
            if( size > MAX_PACKET_SIZE ) throw new DecoderException( "File is too large" );

            ByteBuffer buffer = ByteBuffer.allocateDirect( size );
            buf.readBytes( buffer );
            buffer.flip();

            slices.add( new FileSlice( fileId, offset, buffer ) );
        }
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeUUID( uuid );
        buf.writeByte( flag );

        if( (flag & FLAG_FIRST) != 0 )
        {
            buf.writeVarInt( files.size() );
            for( FileUpload file : files )
            {
                buf.writeUtf( file.getName(), MAX_FILE_NAME );
                buf.writeVarInt( file.getLength() );
                buf.writeBytes( file.getChecksum() );
            }
        }

        buf.writeVarInt( slices.size() );
        for( FileSlice slice : slices )
        {
            buf.writeByte( slice.getFileId() );
            buf.writeVarInt( slice.getOffset() );

            ByteBuffer bytes = slice.getBytes().duplicate();
            buf.writeShort( bytes.remaining() );
            buf.writeBytes( bytes );
        }
    }

    public static void send( Container container, List<FileUpload> files )
    {
        UUID uuid = UUID.randomUUID();

        int remaining = MAX_PACKET_SIZE;
        for( FileUpload file : files ) remaining -= file.getName().length() * 4 + FileUpload.CHECKSUM_LENGTH;

        boolean first = true;
        List<FileSlice> slices = new ArrayList<>( files.size() );
        for( int fileId = 0; fileId < files.size(); fileId++ )
        {
            FileUpload file = files.get( fileId );
            ByteBuffer contents = file.getBytes();
            int capacity = contents.limit();

            int currentOffset = 0;
            while( currentOffset < capacity )
            {
                if( remaining <= 0 )
                {
                    NetworkHandler.sendToServer( first
                        ? new UploadFileMessage( container, uuid, FLAG_FIRST, files, new ArrayList<>( slices ) )
                        : new UploadFileMessage( container, uuid, 0, null, new ArrayList<>( slices ) ) );
                    slices.clear();
                    remaining = MAX_PACKET_SIZE;
                    first = false;
                }

                int canWrite = Math.min( remaining, capacity - currentOffset );

                contents.position( currentOffset ).limit( currentOffset + canWrite );
                slices.add( new FileSlice( fileId, currentOffset, contents.slice() ) );
                currentOffset += canWrite;
                remaining -= canWrite;
            }

            contents.position( 0 ).limit( capacity );
        }

        NetworkHandler.sendToServer( first
            ? new UploadFileMessage( container, uuid, FLAG_FIRST | FLAG_LAST, files, new ArrayList<>( slices ) )
            : new UploadFileMessage( container, uuid, FLAG_LAST, null, new ArrayList<>( slices ) ) );
    }

    @Override
    protected void handle( NetworkEvent.Context context, @Nonnull ComputerMenu container )
    {
        ServerPlayerEntity player = context.getSender();
        if( player != null )
        {
            ServerInputHandler input = container.getInput();
            if( (flag & FLAG_FIRST) != 0 ) input.startUpload( uuid, files );
            input.continueUpload( uuid, slices );
            if( (flag & FLAG_LAST) != 0 ) input.finishUpload( player, uuid );
        }
    }
}
