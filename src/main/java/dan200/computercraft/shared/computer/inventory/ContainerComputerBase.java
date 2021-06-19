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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ContainerComputerBase extends Container implements IContainerComputer
{
    private final Predicate<PlayerEntity> canUse;
    private final IComputer computer;
    private final ComputerFamily family;
    private final InputState input = new InputState( this );

    protected ContainerComputerBase( ContainerType<? extends ContainerComputerBase> type, int id, Predicate<PlayerEntity> canUse, IComputer computer, ComputerFamily family )
    {
        super( type, id );
        this.canUse = canUse;
        this.computer = computer;
        this.family = family;
    }

    protected ContainerComputerBase( ContainerType<? extends ContainerComputerBase> type, int id, PlayerInventory player, ComputerContainerData data )
    {
        this( type, id, x -> true, getComputer( player, data ), data.getFamily() );
    }

    protected static IComputer getComputer( PlayerInventory player, ComputerContainerData data )
    {
        int id = data.getInstanceId();
        if( !player.player.level.isClientSide ) return ComputerCraft.serverComputerRegistry.get( id );

        ClientComputer computer = ComputerCraft.clientComputerRegistry.get( id );
        if( computer == null ) ComputerCraft.clientComputerRegistry.add( id, computer = new ClientComputer( id ) );
        return computer;
    }

    @Override
    public boolean stillValid( @Nonnull PlayerEntity player )
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
    public void upload( @Nonnull ServerPlayerEntity uploader, @Nonnull List<FileUpload> files )
    {
        UploadResultMessage message = upload( files );
        NetworkHandler.sendToPlayer( uploader, message );
    }

    @Nonnull
    private UploadResultMessage upload( @Nonnull List<FileUpload> files )
    {
        ServerComputer computer = (ServerComputer) getComputer();
        if( computer == null ) return UploadResultMessage.COMPUTER_OFF;

        FileSystem fs = computer.getComputer().getEnvironment().getFileSystem();
        if( fs == null ) return UploadResultMessage.COMPUTER_OFF;

        try
        {
            for( FileUpload upload : files )
            {
                if( !fs.exists( upload.getName() ) ) continue;
                // TODO: return fs.isDir( upload.getName() ) ? UploadResult.FAILED : UploadResult.CONFIRM_OVERWRITE;
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
                UploadResult.SUCCESS, new TranslationTextComponent( "gui.computercraft.upload.success.msg", files.size() )
            );
        }
        catch( FileSystemException | IOException e )
        {
            ComputerCraft.log.error( "Error uploading files", e );
            return new UploadResultMessage( UploadResult.ERROR, new TranslationTextComponent( "computercraft.gui.upload.failed.generic", e.getMessage() ) );
        }
    }

    @Override
    public void removed( @Nonnull PlayerEntity player )
    {
        super.removed( player );
        input.close();
    }
}
