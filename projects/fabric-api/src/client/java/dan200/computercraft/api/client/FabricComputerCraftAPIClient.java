// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.client.turtle.RegisterTurtleUpgradeModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.impl.client.FabricComputerCraftAPIClientService;
import net.minecraft.resources.Identifier;

/**
 * The Fabric-specific entrypoint for ComputerCraft's client-side API.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI The main API
 */
public final class FabricComputerCraftAPIClient {
    private FabricComputerCraftAPIClient() {
    }

    /**
     * Register a {@link TurtleUpgradeModel} for a class of turtle upgrades.
     * <p>
     * This may be called at any point after registry creation, though it is recommended to call it within your client
     * setup step.
     * <p>
     * This method may be used as a {@link RegisterTurtleUpgradeModel}, for convenient use in multi-loader code.
     *
     * @param id    The id used for this type of upgrade model.
     * @param codec The codec used to read/decode an upgrade model.
     */
    public static void registerTurtleUpgradeModeller(Identifier id, MapCodec<? extends TurtleUpgradeModel.Unbaked> codec) {
        getInstance().registerTurtleUpgradeModeller(id, codec);
    }

    private static FabricComputerCraftAPIClientService getInstance() {
        return FabricComputerCraftAPIClientService.get();
    }
}
