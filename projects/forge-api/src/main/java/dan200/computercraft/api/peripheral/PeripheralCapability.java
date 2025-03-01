// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * A {@linkplain BlockCapability block capability} for {@link IPeripheral}s. This should be used to register peripherals
 * for a block. It should <em>NOT</em> be used to query peripherals.
 */
public final class PeripheralCapability {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "peripheral");

    private static final BlockCapability<IPeripheral, Direction> capability = BlockCapability.create(ID, IPeripheral.class, Direction.class);

    private PeripheralCapability() {
    }

    public static BlockCapability<IPeripheral, Direction> get() {
        return capability;
    }
}
