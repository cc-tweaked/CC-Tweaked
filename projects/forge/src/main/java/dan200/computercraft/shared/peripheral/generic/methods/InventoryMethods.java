// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.ForgeItemContainer;
import dan200.computercraft.shared.platform.ItemContainer;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
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

        if (object instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved()) return null;

            var level = blockEntity.getLevel();
            if (!(level instanceof ServerLevel serverLevel)) return null;

            var result = CapabilityUtil.getCapability(serverLevel, Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            if (result != null) return result;
        }

        if (object instanceof IItemHandler handler) return handler;
        if (object instanceof Container container) return new InvWrapper(container);
        return null;
    }
}
