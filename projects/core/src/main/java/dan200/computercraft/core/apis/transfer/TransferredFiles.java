// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.transfer;

import dan200.computercraft.api.lua.LuaFunction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A list of files that have been transferred to this computer.
 *
 * @cc.module [kind=event] file_transfer.TransferredFiles
 */
public class TransferredFiles {
    public static final String EVENT = "file_transfer";

    private final AtomicBoolean consumed = new AtomicBoolean(false);
    private final @Nullable Runnable onConsumed;

    private final List<TransferredFile> files;

    public TransferredFiles(List<TransferredFile> files, @Nullable Runnable onConsumed) {
        this.files = files;
        this.onConsumed = onConsumed;
    }

    public TransferredFiles(List<TransferredFile> files) {
        this(files, null);
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
        if (onConsumed != null) onConsumed.run();
    }
}
