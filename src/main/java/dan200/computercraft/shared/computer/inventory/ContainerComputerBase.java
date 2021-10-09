/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.filesystem.FileSystemWrapper;
import dan200.computercraft.shared.computer.core.*;
import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.UploadResultMessage;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class ContainerComputerBase extends ScreenHandler implements IContainerComputer
{
    private static final String LIST_PREFIX = "\n \u2022 ";

    private final Predicate<PlayerEntity> canUse;
    private final IComputer computer;
    private final ComputerFamily family;
    private final InputState input = new InputState( this );

    private UUID toUploadId;
    private List<FileUpload> toUpload;

    public ContainerComputerBase( ScreenHandlerType<? extends ContainerComputerBase> type, int id, PlayerInventory player, ComputerContainerData data )
    {
        this( type,
            id,
            x -> true,
            getComputer( player, data ),
            data.getFamily() );
    }

    public ContainerComputerBase( ScreenHandlerType<? extends ContainerComputerBase> type, int id, Predicate<PlayerEntity> canUse, IComputer computer,
                                     ComputerFamily family )
    {
        super( type, id );
        this.canUse = canUse;
        this.computer = Objects.requireNonNull( computer );
        this.family = family;
    }

    protected static IComputer getComputer( PlayerInventory player, ComputerContainerData data )
    {
        int id = data.getInstanceId();
        if( !player.player.world.isClient )
        {
            return ComputerCraft.serverComputerRegistry.get( id );
        }

        ClientComputer computer = ComputerCraft.clientComputerRegistry.get( id );
        if( computer == null )
        {
            ComputerCraft.clientComputerRegistry.add( id, computer = new ClientComputer( id ) );
        }
        return computer;
    }

    @Nonnull
    public ComputerFamily getFamily()
    {
        return family;
    }

    @Nullable
    @Override
    public IComputer getComputer()
    {
        return computer;
    }

    @Nonnull
    @Override
    public InputState getInput()
    {
        return input;
    }

    @Override
    public void close( @Nonnull PlayerEntity player )
    {
        super.close( player );
        input.close();
    }

    @Override
    public boolean canUse( @Nonnull PlayerEntity player )
    {
        return canUse.test( player );
    }

    @Override
    public void startUpload( @Nonnull UUID uuid, @Nonnull List<FileUpload> files )
    {
        toUploadId = uuid;
        toUpload = files;
    }

    @Override
    public void continueUpload( @Nonnull UUID uploadId, @Nonnull List<FileSlice> slices )
    {
        if( toUploadId == null || toUpload == null || !toUploadId.equals( uploadId ) )
        {
            ComputerCraft.log.warn( "Invalid continueUpload call, skipping." );
            return;
        }

        for( FileSlice slice : slices ) slice.apply( toUpload );
    }

    @Override
    public void finishUpload( @Nonnull ServerPlayerEntity uploader, @Nonnull UUID uploadId )
    {
        if( toUploadId == null || toUpload == null || toUpload.isEmpty() || !toUploadId.equals( uploadId ) )
        {
            ComputerCraft.log.warn( "Invalid finishUpload call, skipping." );
            return;
        }

        UploadResultMessage message = finishUpload( false );
        NetworkHandler.sendToPlayer( uploader, message );
    }

    @Override
    public void confirmUpload( @Nonnull ServerPlayerEntity uploader, boolean overwrite )
    {
        if( toUploadId == null || toUpload == null || toUpload.isEmpty() )
        {
            ComputerCraft.log.warn( "Invalid finishUpload call, skipping." );
            return;
        }

        UploadResultMessage message = finishUpload( true );
        NetworkHandler.sendToPlayer( uploader, message );
    }

    @Nonnull
    private UploadResultMessage finishUpload( boolean forceOverwrite )
    {
        ServerComputer computer = (ServerComputer) getComputer();
        if( computer == null ) return UploadResultMessage.COMPUTER_OFF;

        FileSystem fs = computer.getComputer().getEnvironment().getFileSystem();
        if( fs == null ) return UploadResultMessage.COMPUTER_OFF;

        for( FileUpload upload : toUpload )
        {
            if( !upload.checksumMatches() )
            {
                ComputerCraft.log.warn( "Checksum failed to match for {}.", upload.getName() );
                return new UploadResultMessage( UploadResult.ERROR, new TranslatableText( "gui.computercraft.upload.failed.corrupted" ) );
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
                        new TranslatableText( "gui.computercraft.upload.failed.overwrite_dir", upload.getName() )
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
                    new TranslatableText( "gui.computercraft.upload.overwrite.detail", joiner.toString() )
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
                UploadResult.SUCCESS, new TranslatableText( "gui.computercraft.upload.success.msg", files.size() )
            );
        }
        catch( FileSystemException | IOException e )
        {
            ComputerCraft.log.error( "Error uploading files", e );
            return new UploadResultMessage( UploadResult.ERROR, new TranslatableText( "gui.computercraft.upload.failed.generic", e.getMessage() ) );
        }
    }
}
