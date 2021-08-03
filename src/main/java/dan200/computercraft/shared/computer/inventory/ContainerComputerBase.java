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
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.UploadResultMessage;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;

public class ContainerComputerBase extends AbstractContainerMenu implements IContainerComputer
{
    private static final String LIST_PREFIX = "\n \u2022 ";

    private final Predicate<Player> canUse;
    private final IComputer computer;
    private final ComputerFamily family;
    private final InputState input = new InputState( this );
    private List<FileUpload> toUpload;

    protected ContainerComputerBase( MenuType<? extends ContainerComputerBase> type, int id, Predicate<Player> canUse, IComputer computer, ComputerFamily family )
    {
        super( type, id );
        this.canUse = canUse;
        this.computer = computer;
        this.family = family;
    }

    protected ContainerComputerBase( MenuType<? extends ContainerComputerBase> type, int id, Inventory player, ComputerContainerData data )
    {
        this( type, id, x -> true, getComputer( player, data ), data.getFamily() );
    }

    protected static IComputer getComputer( Inventory player, ComputerContainerData data )
    {
        int id = data.getInstanceId();
        if( !player.player.level.isClientSide ) return ComputerCraft.serverComputerRegistry.get( id );

        ClientComputer computer = ComputerCraft.clientComputerRegistry.get( id );
        if( computer == null ) ComputerCraft.clientComputerRegistry.add( id, computer = new ClientComputer( id ) );
        return computer;
    }

    @Override
    public boolean stillValid( @Nonnull Player player )
    {
        return canUse.test( player );
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
    public void upload( @Nonnull ServerPlayer uploader, @Nonnull List<FileUpload> files )
    {
        UploadResultMessage message = upload( files, false );
        NetworkHandler.sendToPlayer( uploader, message );
    }

    @Override
    public void continueUpload( @Nonnull ServerPlayer uploader, boolean overwrite )
    {
        List<FileUpload> files = this.toUpload;
        toUpload = null;
        if( files == null || files.isEmpty() || !overwrite ) return;

        UploadResultMessage message = upload( files, true );
        NetworkHandler.sendToPlayer( uploader, message );
    }

    @Nonnull
    private UploadResultMessage upload( @Nonnull List<FileUpload> files, boolean forceOverwrite )
    {
        ServerComputer computer = (ServerComputer) getComputer();
        if( computer == null ) return UploadResultMessage.COMPUTER_OFF;

        FileSystem fs = computer.getComputer().getEnvironment().getFileSystem();
        if( fs == null ) return UploadResultMessage.COMPUTER_OFF;

        try
        {
            List<String> overwrite = new ArrayList<>();
            for( FileUpload upload : files )
            {
                if( !fs.exists( upload.getName() ) ) continue;
                if( fs.isDir( upload.getName() ) )
                {
                    return new UploadResultMessage(
                        UploadResult.ERROR,
                        new TranslatableComponent( "gui.computercraft.upload.failed.overwrite_dir", upload.getName() )
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
                    new TranslatableComponent( "gui.computercraft.upload.overwrite.detail", joiner.toString() )
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
                UploadResult.SUCCESS, new TranslatableComponent( "gui.computercraft.upload.success.msg", files.size() )
            );
        }
        catch( FileSystemException | IOException e )
        {
            ComputerCraft.log.error( "Error uploading files", e );
            return new UploadResultMessage( UploadResult.ERROR, new TranslatableComponent( "computercraft.gui.upload.failed.generic", e.getMessage() ) );
        }
    }

    @Override
    public void removed( @Nonnull Player player )
    {
        super.removed( player );
        input.close();
    }
}
