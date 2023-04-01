// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;

public final class IoUtil {
    private IoUtil() {
    }

    public static void closeQuietly(@Nullable Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (IOException ignored) {
            // The whole point here is to suppress these exceptions!
        }
    }
}
