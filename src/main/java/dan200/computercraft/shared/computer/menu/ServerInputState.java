/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.menu;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.upload.*;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.UploadResultMessage;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The default concrete implementation of {@link ServerInputHandler}.
 * <p>
 * This keeps track of the current key and mouse state, and releases them when the container is closed.
 *
 * @param <T> The type of container this server input belongs to.
 */
public class ServerInputState<T extends Container & ComputerMenu> implements ServerInputHandler
{
    private final T owner;
    private final IntSet keysDown = new IntOpenHashSet( 4 );

    private int lastMouseX;
    private int lastMouseY;
    private int lastMouseDown = -1;

    private @Nullable UUID toUploadId;
    private @Nullable List<FileUpload> toUpload;

    public ServerInputState( T owner )
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
    public void finishUpload( ServerPlayerEntity uploader, UUID uploadId )
    {
        if( toUploadId == null || toUpload == null || toUpload.isEmpty() || !toUploadId.equals( uploadId ) )
        {
            ComputerCraft.log.warn( "Invalid finishUpload call, skipping." );
            return;
        }

        NetworkMessage message = finishUpload( uploader );
        NetworkHandler.sendToPlayer( uploader, message );
    }

    private UploadResultMessage finishUpload( ServerPlayerEntity player )
    {
        ServerComputer computer = owner.getComputer();
        if( toUpload == null )
        {
            return UploadResultMessage.error( owner, UploadResult.COMPUTER_OFF_MSG );
        }

        for( FileUpload upload : toUpload )
        {
            if( !upload.checksumMatches() )
            {
                ComputerCraft.log.warn( "Checksum failed to match for {}.", upload.getName() );
                return UploadResultMessage.error( owner, new TranslationTextComponent( "gui.computercraft.upload.failed.corrupted" ) );
            }
        }

        computer.queueEvent( "file_transfer", new Object[] {
            new FileTransfer( player, owner, toUpload.stream().map( x -> new TransferredFile( x.getName(), x.getBytes() ) ).collect( Collectors.toList() ) ),
        } );
        return UploadResultMessage.queued( owner );
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
