// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.impl.ComputerCraftAPIForgeService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * The forge-specific entrypoint for ComputerCraft's API.
 */
public final class ForgeComputerCraftAPI {
    private ForgeComputerCraftAPI() {
    }

    /**
     * Registers a peripheral provider to convert blocks into {@link IPeripheral} implementations.
     *
     * @param provider The peripheral provider to register.
     * @see IPeripheral
     * @see IPeripheralProvider
     */
    public static void registerPeripheralProvider(IPeripheralProvider provider) {
        getInstance().registerPeripheralProvider(provider);
    }

    /**
     * Registers a capability that can be used by generic peripherals.
     *
     * @param capability The capability to register.
     * @see GenericSource
     */
    public static void registerGenericCapability(Capability<?> capability) {
        getInstance().registerGenericCapability(capability);
    }

    /**
     * Get the wired network element for a block in world.
     *
     * @param world The world the block exists in
     * @param pos   The position the block exists in
     * @param side  The side to extract the network element from
     * @return The element's node
     * @see WiredElement#getNode()
     */
    public static LazyOptional<WiredElement> getWiredElementAt(BlockGetter world, BlockPos pos, Direction side) {
        return getInstance().getWiredElementAt(world, pos, side);
    }

    private static ComputerCraftAPIForgeService getInstance() {
        return ComputerCraftAPIForgeService.get();
    }
}
