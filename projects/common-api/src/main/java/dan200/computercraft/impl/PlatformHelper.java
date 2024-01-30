// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import com.google.gson.JsonObject;
import dan200.computercraft.api.upgrades.UpgradeDataProvider;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * Abstraction layer for Forge and Fabric. See implementations for more details.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface PlatformHelper {
    /**
     * Get the current {@link PlatformHelper} instance.
     *
     * @return The current instance.
     */
    static PlatformHelper get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(PlatformHelper.class, Instance.ERROR) : instance;
    }

    /**
     * Add a resource condition which requires a mod to be loaded. This should be used by data providers such as
     * {@link UpgradeDataProvider}.
     *
     * @param object The JSON object we're generating.
     * @param modId  The mod ID that we require.
     */
    void addRequiredModCondition(JsonObject object, String modId);

    final class Instance {
        static final @Nullable PlatformHelper INSTANCE;
        static final @Nullable Throwable ERROR;

        static {
            // We don't want class initialisation to fail here (as that results in confusing errors). Instead, capture
            // the error and rethrow it when accessing. This should be JITted away in the common case.
            var helper = Services.tryLoad(PlatformHelper.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance() {
        }
    }
}
