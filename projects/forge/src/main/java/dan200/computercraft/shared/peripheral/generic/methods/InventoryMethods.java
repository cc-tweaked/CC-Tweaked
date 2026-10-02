// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.ForgeItemContainer;
import dan200.computercraft.shared.platform.ItemContainer;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jspecify.annotations.Nullable;

/**
 * Inventory methods for Forge's {@link IItemHandler}.
 */
public final class InventoryMethods extends AbstractInventoryMethods<IItemHandler> {
    @Override
    protected ItemContainer getContainer(IItemHandler inventory) {
        return new ForgeItemContainer(inventory);
    }

    @Override
    protected @Nullable ItemContainer getContainer(IPeripheral peripheral) {
        var handler = extractHandler(peripheral);
        return handler == null ? null : new ForgeItemContainer(handler);
    }

    @Nullable
    private static IItemHandler extractHandler(IPeripheral peripheral) {
        var object = peripheral.getTarget();
        var direction = peripheral instanceof dan200.computercraft.shared.peripheral.generic.GenericPeripheral sided ? sided.side() : null;

        if (object instanceof BlockEntity blockEntity && blockEntity.isRemoved()) return null;

        if (object instanceof ICapabilityProvider provider) {
            var cap = CapabilityUtil.getCapability(provider, ForgeCapabilities.ITEM_HANDLER, direction);
            if (cap.isPresent()) return cap.orElseThrow(NullPointerException::new);
        }

        if (object instanceof IItemHandler handler) return handler;
        if (object instanceof Container container) return new InvWrapper(container);
        return null;
    }
}
