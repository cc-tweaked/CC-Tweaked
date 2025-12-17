// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.client;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Backing interface for CC's client-side API.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface FabricComputerCraftAPIClientService extends ComputerCraftAPIClientService {
    static FabricComputerCraftAPIClientService get() {
        return (FabricComputerCraftAPIClientService) ComputerCraftAPIClientService.get();
    }

    void registerTurtleUpgradeModeller(Identifier id, MapCodec<? extends TurtleUpgradeModel.Unbaked> codec);
}
