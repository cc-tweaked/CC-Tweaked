/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * An instance of {@link AbstractContainerMenu} which provides a computer. You should implement this
 * if you provide custom computers/GUIs to interact with them.
 */
public interface IContainerComputer
{
    /**
     * Get the computer you are interacting with.
     *
     * This will only be called on the server.
     *
     * @return The computer you are interacting with.
     */
    @Nullable
    IComputer getComputer();

    /**
     * Get the input controller for this container.
     *
     * @return This container's input.
     */
    @Nonnull
    InputState getInput();

    /**
     * Start a file upload into this container.
     *
     * @param uploadId The unique ID of this upload.
     * @param files    The files to upload.
     */
    void startUpload( @Nonnull UUID uploadId, @Nonnull List<FileUpload> files );

    /**
     * Append more data to partially uploaded files.
     *
     * @param uploadId The unique ID of this upload.
     * @param slices   Additional parts of file data to upload.
     */
    void continueUpload( @Nonnull UUID uploadId, @Nonnull List<FileSlice> slices );

    /**
     * Finish off an upload. This either writes the uploaded files or informs the user that files will be overwritten.
     *
     * @param uploader The player uploading files.
     * @param uploadId The unique ID of this upload.
     */
    void finishUpload( @Nonnull ServerPlayer uploader, @Nonnull UUID uploadId );

    /**
     * Continue an upload.
     *
     * @param uploader  The player uploading files.
     * @param overwrite Whether the files should be overwritten or not.
     */
    void confirmUpload( @Nonnull ServerPlayer uploader, boolean overwrite );
}
