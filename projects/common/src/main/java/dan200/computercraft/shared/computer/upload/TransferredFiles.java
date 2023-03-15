// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.upload;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.shared.network.client.UploadResultMessage;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A list of files that have been transferred to this computer.
 *
 * @cc.module [kind=event] file_transfer.TransferredFiles
 */
public class TransferredFiles {
    private final ServerPlayer player;
    private final AbstractContainerMenu container;
    private final AtomicBoolean consumed = new AtomicBoolean(false);

    private final List<TransferredFile> files;

    public TransferredFiles(ServerPlayer player, AbstractContainerMenu container, List<TransferredFile> files) {
        this.player = player;
        this.container = container;
        this.files = files;
    }

    /**
     * All the files that are being transferred to this computer.
     *
     * @return The list of files.
     */
    @LuaFunction
    public final List<TransferredFile> getFiles() {
        consumed();
        return files;
    }

    private void consumed() {
        if (consumed.getAndSet(true)) return;

        if (player.isAlive() && player.containerMenu == container) {
            PlatformHelper.get().sendToPlayer(UploadResultMessage.consumed(container), player);
        }
    }
}
