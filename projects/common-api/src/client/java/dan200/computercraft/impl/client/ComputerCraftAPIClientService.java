// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.client;

import com.mojang.serialization.Codec;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.impl.Services;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Bridge between implementation
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface ComputerCraftAPIClientService {
    static ComputerCraftAPIClientService get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(ComputerCraftAPIClientService.class, Instance.ERROR) : instance;
    }

    Codec<TurtleUpgradeModel.Unbaked> getTurtleUpgradeModelCodec();

    final class Instance {
        static final @Nullable ComputerCraftAPIClientService INSTANCE;
        static final @Nullable Throwable ERROR;

        static {
            var helper = Services.tryLoad(ComputerCraftAPIClientService.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance() {
        }
    }
}
