// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.peripheral.block.BlockPeripheralTarget;
import dan200.computercraft.api.peripheral.block.PeripheralTargets;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
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
     * Get a capability from an {@link IPeripheral}.
     * <p>
     * This function will attempt to find the capability on the internal/{@code null} side of the block first,
     * falling back to the {@linkplain BlockPeripheralTarget#getSide() side the peripheral is attached on} if known.
     *
     * @param peripheral The peripheral to extract the capability from.
     * @param capability The capability to get.
     * @param <T>        The type of the capability.
     * @return The extracted capability.
     */
    public static <T> LazyOptional<T> getCapability(IPeripheral peripheral, Capability<T> capability) {
        return getCapability(peripheral.getTarget(), PeripheralTargets.getBlockTarget(peripheral), capability);
    }

    private static <T> LazyOptional<T> getCapability(@Nullable Object target, @Nullable BlockPeripheralTarget blockTarget, Capability<T> capability) {
        if (blockTarget != null && blockTarget.getBlockEntity() != null) {
            var cap = blockTarget.getBlockEntity().getCapability(capability, null);
            if (cap.isPresent()) return cap;

            if (blockTarget.getSide() != null) {
                var sidedCap = blockTarget.getBlockEntity().getCapability(capability, blockTarget.getSide());
                if (sidedCap.isPresent()) return sidedCap;
            }
        }

        if (target instanceof ICapabilityProvider provider) {
            var cap = provider.getCapability(capability, null);
            if (cap.isPresent()) return cap;
        }

        return LazyOptional.empty();
    }

    /**
     * Get an {@link IItemHandler} from an {@link IPeripheral}.
     * <p>
     * This function will attempt to find the item handler on the internal/{@code null} side of the block first,
     * falling back to the {@linkplain BlockPeripheralTarget#getSide() side the peripheral is attached on} if known.
     *
     * @param peripheral The peripheral to extract the capability from.
     * @return The item handler, if present.
     */
    public static @Nullable IItemHandler getItemHandler(IPeripheral peripheral) {
        var target = peripheral.getTarget();
        if (target == null) return null;

        var blockTarget = PeripheralTargets.getBlockTarget(peripheral);

        var cap = getCapability(target, blockTarget, ForgeCapabilities.ITEM_HANDLER);
        if (cap.isPresent()) return cap.orElseThrow(NullPointerException::new);

        // If we're a WorldlyContainerHolder, extract from that.
        if (blockTarget != null) {
            var block = blockTarget.getBlockState();
            if (block.getBlock() instanceof WorldlyContainerHolder holder) {
                var container = holder.getContainer(block, blockTarget.getLevel(), blockTarget.getPos());
                return new SidedInvWrapper(container, blockTarget.getSide());
            }
        }

        // Some fallbacks to primitive types. I don't think this is actually needed, but
        if (target instanceof IItemHandler handler) return handler;
        if (target instanceof Container container) return new InvWrapper(container);

        return null;
    }

    /**
     * Get an {@link IFluidHandler} from an {@link IPeripheral}.
     * <p>
     * This function will attempt to find the fluid handler on the internal/{@code null} side of the block first,
     * falling back to the {@linkplain BlockPeripheralTarget#getSide() side the peripheral is attached on} if known.
     *
     * @param peripheral The peripheral to extract the capability from.
     * @return The fluid handler, if present.
     */
    public static @Nullable IFluidHandler getFluidHandler(IPeripheral peripheral) {
        var target = peripheral.getTarget();
        if (target == null) return null;

        var blockTarget = PeripheralTargets.getBlockTarget(peripheral);

        var cap = getCapability(target, blockTarget, ForgeCapabilities.FLUID_HANDLER);
        if (cap.isPresent()) return cap.orElseThrow(NullPointerException::new);

        if (target instanceof IFluidHandler handler) return handler;
        return null;
    }
}
