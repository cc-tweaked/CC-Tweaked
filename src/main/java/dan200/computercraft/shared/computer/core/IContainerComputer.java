/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import dan200.computercraft.shared.computer.upload.FileUpload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

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
     * Attempt to upload a series of files to this computer.
     *
     * @param uploader The player uploading files.
     * @param files    The files to upload.
     */
    void upload( @Nonnull ServerPlayer uploader, @Nonnull List<FileUpload> files );

    /**
     * Continue an upload.
     *
     * @param uploader  The player uploading files.
     * @param overwrite Whether the files should be overwritten or not.
     */
    void continueUpload( @Nonnull ServerPlayer uploader, boolean overwrite );
}
