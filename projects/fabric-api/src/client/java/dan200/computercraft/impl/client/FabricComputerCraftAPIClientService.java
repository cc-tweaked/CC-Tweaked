// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.client;

import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.impl.Services;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * Backing interface for CC's client-side API.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface FabricComputerCraftAPIClientService {
    static FabricComputerCraftAPIClientService get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(FabricComputerCraftAPIClientService.class, Instance.ERROR) : instance;
    }

    <T extends ITurtleUpgrade> void registerTurtleUpgradeModeller(UpgradeSerialiser<T> serialiser, TurtleUpgradeModeller<T> modeller);

    final class Instance {
        static final @Nullable FabricComputerCraftAPIClientService INSTANCE;
        static final @Nullable Throwable ERROR;

        static {
            var helper = Services.tryLoad(FabricComputerCraftAPIClientService.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance() {
        }
    }
}
