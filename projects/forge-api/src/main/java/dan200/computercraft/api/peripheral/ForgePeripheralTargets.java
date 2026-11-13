// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.peripheral.block.BlockPeripheralTarget;
import dan200.computercraft.api.peripheral.block.PeripheralTargets;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.jspecify.annotations.Nullable;

/**
 * Forge-specific helpers for extracting objects from the {@linkplain IPeripheral#getTarget() target} from a peripheral.
 *
 * @see PeripheralTargets
 * @since 1.122.0
 */
public final class ForgePeripheralTargets {
    private ForgePeripheralTargets() {
    }

    /**
     * Get a sided capability from an {@link IPeripheral}.
     * <p>
     * This function will attempt to find the capability on the internal/{@code null} side of the block first,
     * falling back to the {@linkplain BlockPeripheralTarget#getSide() side the peripheral is attached on} if known.
     *
     * @param peripheral The peripheral to extract the capability from.
     * @param capability The sided capability to get.
     * @param <T>        The type of the capability.
     * @return The extracted capability.
     */
    public static <T> @Nullable T getSidedCapability(IPeripheral peripheral, BlockCapability<T, @Nullable Direction> capability) {
        var blockTarget = PeripheralTargets.getBlockTarget(peripheral);
        if (blockTarget == null) return null;

        var level = blockTarget.getLevel();
        var blockState = blockTarget.getBlockState();

        var result = level.getCapability(capability, blockTarget.getPos(), blockState, blockTarget.getBlockEntity(), null);
        if (result != null) return result;

        if (blockTarget.getSide() != null) {
            var sidedResult = level.getCapability(capability, blockTarget.getPos(), blockState, blockTarget.getBlockEntity(), blockTarget.getSide());
            if (sidedResult != null) return sidedResult;
        }

        return null;
    }

    /**
     * Get an item {@link ResourceHandler} from an {@link IPeripheral}.
     * <p>
     * This function will attempt to find the item handler on the internal/{@code null} side of the block first,
     * falling back to the {@linkplain BlockPeripheralTarget#getSide() side the peripheral is attached on} if known.
     *
     * @param peripheral The peripheral to extract the capability from.
     * @return The item handler, if present.
     */
    public static @Nullable ResourceHandler<ItemResource> getItemHandler(IPeripheral peripheral) {
        var target = peripheral.getTarget();
        if (target == null) return null;

        var cap = getSidedCapability(peripheral, Capabilities.Item.BLOCK);
        if (cap != null) return cap;

        // Some fallbacks to primitive types. I don't think this is actually needed, but
        if (target instanceof Container container) return VanillaContainerWrapper.of(container);

        return null;
    }

    /**
     * Get a fluid {@link ResourceHandler} from an {@link IPeripheral}.
     * <p>
     * This function will attempt to find the fluid handler on the internal/{@code null} side of the block first,
     * falling back to the {@linkplain BlockPeripheralTarget#getSide() side the peripheral is attached on} if known.
     *
     * @param peripheral The peripheral to extract the capability from.
     * @return The fluid handler, if present.
     */
    public static @Nullable ResourceHandler<FluidResource> getFluidHandler(IPeripheral peripheral) {
        return getSidedCapability(peripheral, Capabilities.Fluid.BLOCK);
    }
}
