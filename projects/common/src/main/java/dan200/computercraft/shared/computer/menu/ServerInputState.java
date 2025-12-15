// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.menu;

import dan200.computercraft.core.apis.handles.ByteBufferChannel;
import dan200.computercraft.core.apis.transfer.TransferredFile;
import dan200.computercraft.core.apis.transfer.TransferredFiles;
import dan200.computercraft.core.input.ComputerInput;
import dan200.computercraft.core.input.UserComputerInput;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.client.UploadResultMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * The default concrete implementation of {@link ServerInputHandler}.
 * <p>
 * This keeps track of the current key and mouse state, and releases them when the container is closed.
 */
public class ServerInputState implements ServerInputHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ServerInputState.class);

    private final AbstractContainerMenu owner;
    private final ServerComputer computer;
    private final UserComputerInput input;

    private @Nullable UUID toUploadId;
    private @Nullable List<FileUpload> toUpload;

    public ServerInputState(AbstractContainerMenu owner, ServerComputer computer) {
        this.owner = owner;
        this.computer = computer;
        this.input = computer.createComputerInput();
    }

    @Override
    public ComputerInput getComputerInput() {
        return input;
    }

    @Override
    public void startUpload(UUID uploadId, List<FileUpload> files) {
        toUploadId = uploadId;
        toUpload = files;
    }

    @Override
    public void continueUpload(UUID uploadId, List<FileSlice> slices) {
        if (toUploadId == null || toUpload == null || !toUploadId.equals(uploadId)) {
            LOG.warn("Invalid continueUpload call, skipping.");
            return;
        }

        for (var slice : slices) slice.apply(toUpload);
    }

    @Override
    public void finishUpload(ServerPlayer uploader, UUID uploadId) {
        if (toUploadId == null || toUpload == null || toUpload.isEmpty() || !toUploadId.equals(uploadId)) {
            LOG.warn("Invalid finishUpload call, skipping.");
            return;
        }

        ServerNetworking.sendToPlayer(finishUpload(uploader), uploader);
    }

    private UploadResultMessage finishUpload(ServerPlayer player) {
        if (toUpload == null) {
            return UploadResultMessage.error(owner, UploadResult.COMPUTER_OFF_MSG);
        }

        for (var upload : toUpload) {
            if (!upload.checksumMatches()) {
                LOG.warn("Checksum failed to match for {}.", upload.getName());
                return UploadResultMessage.error(owner, Component.translatable("gui.computercraft.upload.failed.corrupted"));
            }
        }

        computer.queueEvent(TransferredFiles.EVENT, new Object[]{
            new TransferredFiles(
                toUpload.stream().map(x -> new TransferredFile(x.getName(), new ByteBufferChannel(x.getBytes()))).toList(),
                () -> {
                    if (player.isAlive() && player.containerMenu == owner) {
                        ServerNetworking.sendToPlayer(UploadResultMessage.consumed(owner), player);
                    }
                }),
        });
        return UploadResultMessage.queued(owner);
    }

    public void close() {
        input.releaseInputs();
    }
}
