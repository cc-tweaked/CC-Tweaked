/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.menu;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.filesystem.FileSystemWrapper;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.UploadResultMessage;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Function;

/**
 * The default concrete implementation of {@link ServerInputHandler}.
 * <p>
 * This keeps track of the current key and mouse state, and releases them when the container is closed.
 */
public class ServerInputState implements ServerInputHandler
{
    private static final String LIST_PREFIX = "\n \u2022 ";

    private final ComputerMenu owner;
    private final IntSet keysDown = new IntOpenHashSet( 4 );

    private int lastMouseX;
    private int lastMouseY;
    private int lastMouseDown = -1;

    private @Nullable UUID toUploadId;
    private @Nullable List<FileUpload> toUpload;

    public ServerInputState( ComputerMenu owner )
    {
        this.owner = owner;
    }

    @Override
    public void queueEvent( String event, @Nullable Object[] arguments )
    {
        owner.getComputer().queueEvent( event, arguments );
    }

    @Override
    public void keyDown( int key, boolean repeat )
    {
        keysDown.add( key );
        owner.getComputer().keyDown( key, repeat );
    }

    @Override
    public void keyUp( int key )
    {
        keysDown.remove( key );
        owner.getComputer().keyUp( key );
    }

    @Override
    public void mouseClick( int button, int x, int y )
    {
        lastMouseX = x;
        lastMouseY = y;
        lastMouseDown = button;

        owner.getComputer().mouseClick( button, x, y );
    }

    @Override
    public void mouseUp( int button, int x, int y )
    {
        lastMouseX = x;
        lastMouseY = y;
        lastMouseDown = -1;

        owner.getComputer().mouseUp( button, x, y );
    }

    @Override
    public void mouseDrag( int button, int x, int y )
    {
        lastMouseX = x;
        lastMouseY = y;
        lastMouseDown = button;

        owner.getComputer().mouseDrag( button, x, y );
    }

    @Override
    public void mouseScroll( int direction, int x, int y )
    {
        lastMouseX = x;
        lastMouseY = y;

        owner.getComputer().mouseScroll( direction, x, y );
    }

    @Override
    public void shutdown()
    {
        owner.getComputer().shutdown();
    }

    @Override
    public void turnOn()
    {
        owner.getComputer().turnOn();
    }

    @Override
    public void reboot()
    {
        owner.getComputer().reboot();
    }

    @Override
    public void startUpload( UUID uuid, List<FileUpload> files )
    {
        toUploadId = uuid;
        toUpload = files;
    }

    @Override
    public void continueUpload( UUID uploadId, List<FileSlice> slices )
    {
        if( toUploadId == null || toUpload == null || !toUploadId.equals( uploadId ) )
        {
            ComputerCraft.log.warn( "Invalid continueUpload call, skipping." );
            return;
        }

        for( FileSlice slice : slices ) slice.apply( toUpload );
    }

    @Override
    public void finishUpload( ServerPlayer uploader, UUID uploadId )
    {
        if( toUploadId == null || toUpload == null || toUpload.isEmpty() || !toUploadId.equals( uploadId ) )
        {
            ComputerCraft.log.warn( "Invalid finishUpload call, skipping." );
            return;
        }

        NetworkMessage message = finishUpload( false );
        NetworkHandler.sendToPlayer( uploader, message );
    }

    @Override
    public void confirmUpload( ServerPlayer uploader, boolean overwrite )
    {
        if( toUploadId == null || toUpload == null || toUpload.isEmpty() )
        {
            ComputerCraft.log.warn( "Invalid finishUpload call, skipping." );
            return;
        }

        NetworkMessage message = finishUpload( true );
        NetworkHandler.sendToPlayer( uploader, message );
    }

    private UploadResultMessage finishUpload( boolean forceOverwrite )
    {
        ServerComputer computer = owner.getComputer();
        if( toUpload == null ) return UploadResultMessage.COMPUTER_OFF;

        FileSystem fs = computer.getComputer().getAPIEnvironment().getFileSystem();

        for( FileUpload upload : toUpload )
        {
            if( !upload.checksumMatches() )
            {
                ComputerCraft.log.warn( "Checksum failed to match for {}.", upload.getName() );
                return new UploadResultMessage( UploadResult.ERROR, Component.translatable( "gui.computercraft.upload.failed.corrupted" ) );
            }
        }

        try
        {
            List<String> overwrite = new ArrayList<>();
            List<FileUpload> files = toUpload;
            toUpload = null;
            for( FileUpload upload : files )
            {
                if( !fs.exists( upload.getName() ) ) continue;
                if( fs.isDir( upload.getName() ) )
                {
                    return new UploadResultMessage(
                        UploadResult.ERROR,
                        Component.translatable( "gui.computercraft.upload.failed.overwrite_dir", upload.getName() )
                    );
                }

                overwrite.add( upload.getName() );
            }

            if( !overwrite.isEmpty() && !forceOverwrite )
            {
                StringJoiner joiner = new StringJoiner( LIST_PREFIX, LIST_PREFIX, "" );
                for( String value : overwrite ) joiner.add( value );
                toUpload = files;
                return new UploadResultMessage(
                    UploadResult.CONFIRM_OVERWRITE,
                    Component.translatable( "gui.computercraft.upload.overwrite.detail", joiner.toString() )
                );
            }

            long availableSpace = fs.getFreeSpace( "/" );
            long neededSpace = 0;
            for( FileUpload upload : files ) neededSpace += Math.max( 512, upload.getBytes().remaining() );
            if( neededSpace > availableSpace ) return UploadResultMessage.OUT_OF_SPACE;

            for( FileUpload file : files )
            {
                try( FileSystemWrapper<WritableByteChannel> channel = fs.openForWrite( file.getName(), false, Function.identity() ) )
                {
                    channel.get().write( file.getBytes() );
                }
            }

            return new UploadResultMessage(
                UploadResult.SUCCESS, Component.translatable( "gui.computercraft.upload.success.msg", files.size() )
            );
        }
        catch( FileSystemException | IOException e )
        {
            ComputerCraft.log.error( "Error uploading files", e );
            return new UploadResultMessage( UploadResult.ERROR, Component.translatable( "gui.computercraft.upload.failed.generic", e.getMessage() ) );
        }
    }

    public void close()
    {
        ServerComputer computer = owner.getComputer();
        IntIterator keys = keysDown.iterator();
        while( keys.hasNext() ) computer.keyUp( keys.nextInt() );

        if( lastMouseDown != -1 ) computer.mouseUp( lastMouseDown, lastMouseX, lastMouseY );

        keysDown.clear();
        lastMouseDown = -1;
    }
}
