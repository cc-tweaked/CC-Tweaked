// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.menu;

import dan200.computercraft.shared.computer.core.InputHandler;
import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.network.server.ComputerServerMessage;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * An {@link InputHandler} which operates on the server, receiving data from the client over the network.
 *
 * @see ServerInputState The default implementation of this interface.
 * @see ComputerServerMessage Packets which consume this interface.
 * @see ComputerMenu
 */
public interface ServerInputHandler extends InputHandler {
    /**
     * Start a file upload into this container.
     *
     * @param uploadId The unique ID of this upload.
     * @param files    The files to upload.
     */
    void startUpload(UUID uploadId, List<FileUpload> files);

    /**
     * Append more data to partially uploaded files.
     *
     * @param uploadId The unique ID of this upload.
     * @param slices   Additional parts of file data to upload.
     */
    void continueUpload(UUID uploadId, List<FileSlice> slices);

    /**
     * Finish off an upload. This either writes the uploaded files or informs the user that files will be overwritten.
     *
     * @param uploader The player uploading files.
     * @param uploadId The unique ID of this upload.
     */
    void finishUpload(ServerPlayer uploader, UUID uploadId);
}
