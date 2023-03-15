// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.client;

import dan200.computercraft.api.client.ComputerCraftAPIClient;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.impl.Services;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * Backing interface for {@link ComputerCraftAPIClient}
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface ComputerCraftAPIClientService {
    static ComputerCraftAPIClientService get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(ComputerCraftAPIClientService.class, Instance.ERROR) : instance;
    }

    <T extends ITurtleUpgrade> void registerTurtleUpgradeModeller(TurtleUpgradeSerialiser<T> serialiser, TurtleUpgradeModeller<T> modeller);

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
