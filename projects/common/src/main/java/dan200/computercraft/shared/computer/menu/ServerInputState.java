// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.menu;

import dan200.computercraft.core.apis.handles.ByteBufferChannel;
import dan200.computercraft.core.apis.transfer.TransferredFile;
import dan200.computercraft.core.apis.transfer.TransferredFiles;
import dan200.computercraft.shared.computer.upload.FileSlice;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.client.UploadResultMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The default concrete implementation of {@link ServerInputHandler}.
 * <p>
 * This keeps track of the current key and mouse state, and releases them when the container is closed.
 *
 * @param <T> The type of container this server input belongs to.
 */
public class ServerInputState<T extends AbstractContainerMenu & ComputerMenu> implements ServerInputHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ServerInputState.class);

    private final T owner;
    private final IntSet keysDown = new IntOpenHashSet(4);

    private int lastMouseX;
    private int lastMouseY;
    private int lastMouseDown = -1;

    private @Nullable UUID toUploadId;
    private @Nullable List<FileUpload> toUpload;

    public ServerInputState(T owner) {
        this.owner = owner;
    }

    @Override
    public void queueEvent(String event, @Nullable Object[] arguments) {
        owner.getComputer().queueEvent(event, arguments);
    }

    @Override
    public void keyDown(int key, boolean repeat) {
        keysDown.add(key);
        owner.getComputer().keyDown(key, repeat);
    }

    @Override
    public void keyUp(int key) {
        keysDown.remove(key);
        owner.getComputer().keyUp(key);
    }

    @Override
    public void mouseClick(int button, int x, int y) {
        lastMouseX = x;
        lastMouseY = y;
        lastMouseDown = button;

        owner.getComputer().mouseClick(button, x, y);
    }

    @Override
    public void mouseUp(int button, int x, int y) {
        lastMouseX = x;
        lastMouseY = y;
        lastMouseDown = -1;

        owner.getComputer().mouseUp(button, x, y);
    }

    @Override
    public void mouseDrag(int button, int x, int y) {
        lastMouseX = x;
        lastMouseY = y;
        lastMouseDown = button;

        owner.getComputer().mouseDrag(button, x, y);
    }

    @Override
    public void mouseScroll(int direction, int x, int y) {
        lastMouseX = x;
        lastMouseY = y;

        owner.getComputer().mouseScroll(direction, x, y);
    }

    @Override
    public void shutdown() {
        owner.getComputer().shutdown();
    }

    @Override
    public void turnOn() {
        owner.getComputer().turnOn();
    }

    @Override
    public void reboot() {
        owner.getComputer().reboot();
    }

    @Override
    public void startUpload(UUID uuid, List<FileUpload> files) {
        toUploadId = uuid;
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
        var computer = owner.getComputer();
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
        var computer = owner.getComputer();
        var keys = keysDown.iterator();
        while (keys.hasNext()) computer.keyUp(keys.nextInt());

        if (lastMouseDown != -1) computer.mouseUp(lastMouseDown, lastMouseX, lastMouseY);

        keysDown.clear();
        lastMouseDown = -1;
    }
}
