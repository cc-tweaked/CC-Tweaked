// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.filesystem;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serial;
import java.util.Objects;

/**
 * An {@link IOException} which occurred on a specific file.
 * <p>
 * This may be thrown from a {@link Mount} or {@link WritableMount} to give more information about a failure.
 */
public class FileOperationException extends IOException {
    @Serial
    private static final long serialVersionUID = -8809108200853029849L;

    private final @Nullable String filename;

    public FileOperationException(@Nullable String filename, String message) {
        super(Objects.requireNonNull(message, "message cannot be null"));
        this.filename = filename;
    }

    public FileOperationException(String message) {
        super(Objects.requireNonNull(message, "message cannot be null"));
        filename = null;
    }

    @Nullable
    public String getFilename() {
        return filename;
    }
}
