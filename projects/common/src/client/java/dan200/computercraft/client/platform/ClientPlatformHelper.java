// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import dan200.computercraft.impl.Services;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public interface ClientPlatformHelper {
    static ClientPlatformHelper get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(ClientPlatformHelper.class, Instance.ERROR) : instance;
    }

    /**
     * Create a new unique {@link ModelKey}.
     *
     * @param id   An identifier for this model key.
     * @param name The debug name for this model key.
     * @param <T>  The type of baked model.
     * @return The newly created model key.
     */
    @Contract("_, _ -> new")
    <T> ModelKey<T> createModelKey(ResourceLocation id, ModelDebugName name);

    final class Instance {
        static final @Nullable ClientPlatformHelper INSTANCE;
        static final @Nullable Throwable ERROR;

        static {
            var helper = Services.tryLoad(ClientPlatformHelper.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance() {
        }
    }
}
