// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.impl.ComputerCraftAPIFabricService;
import net.minecraft.resources.ResourceLocation;

/**
 * The fabric-specific entrypoint for ComputerCraft's API.
 */
public final class FabricComputerCraftAPI {
    private static ComputerCraftAPIFabricService getInstance() {
        return ComputerCraftAPIFabricService.get();
    }

    /**
     * Registers a turtle upgrade serializer.
     *
     * @param key Turtle upgrade serializer ID.
     * @param serializer Turtle upgrade serializer for registration
     * @param <T> Turtle upgrade type for registration
     * @see ITurtleUpgrade
     * @see TurtleUpgradeSerialiser
     * @return registered turtle upgrade serializer
     */
    public <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> registerTurtleSerializer(ResourceLocation key, TurtleUpgradeSerialiser<T> serializer) {
        return getInstance().registerTurtleSerializer(key, serializer);
    }
    /**
     * Registers a pocket upgrade serializer.
     *
     * @param key Pocket upgrade serializer ID.
     * @param serializer Pocket upgrade serializer for registration
     * @param <T> Pocket upgrade type for registration
     * @see IPocketUpgrade
     * @see PocketUpgradeSerialiser
     * @return registered pocket upgrade serializer
     */
    public <T extends IPocketUpgrade> PocketUpgradeSerialiser<T> registerPocketSerializer(ResourceLocation key, PocketUpgradeSerialiser<T> serializer) {
        return getInstance().registerPocketSerializer(key, serializer);
    }
}
