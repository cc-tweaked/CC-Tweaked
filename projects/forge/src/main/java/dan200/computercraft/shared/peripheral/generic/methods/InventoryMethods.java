// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import com.google.common.annotations.VisibleForTesting;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.ForgeItemContainer;
import dan200.computercraft.shared.platform.ItemContainer;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.jspecify.annotations.Nullable;

/**
 * Inventory methods for Forge's {@link ResourceHandler}.
 */
public final class InventoryMethods extends AbstractInventoryMethods<InventoryMethods.StorageWrapper> {
    public InventoryMethods(MinecraftServer server) {
        this(server.registryAccess());
    }

    @VisibleForTesting
    InventoryMethods(HolderLookup.Provider registries) {
        super(registries);
    }

    public record StorageWrapper(ResourceHandler<ItemResource> storage) {
    }

    @Override
    protected ItemContainer getContainer(StorageWrapper inventory) {
        return new ForgeItemContainer(inventory.storage());
    }

    @Override
    protected @Nullable ItemContainer getContainer(IPeripheral peripheral) {
        var handler = extractHandler(peripheral);
        return handler == null ? null : new ForgeItemContainer(handler);
    }

    public static @Nullable StorageWrapper extractContainer(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Direction direction) {
        var storage = CapabilityUtil.getCapability(level, Capabilities.Item.BLOCK, pos, state, blockEntity, direction);
        return storage == null ? null : new StorageWrapper(storage);
    }

    @Nullable
    private static ResourceHandler<ItemResource> extractHandler(IPeripheral peripheral) {
        var object = peripheral.getTarget();
        var direction = peripheral instanceof dan200.computercraft.shared.peripheral.generic.GenericPeripheral sided ? sided.side() : null;

        if (object instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved()) return null;

            var level = blockEntity.getLevel();
            if (!(level instanceof ServerLevel serverLevel)) return null;

            var result = CapabilityUtil.getCapability(serverLevel, Capabilities.Item.BLOCK, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            if (result != null) return result;
        }

        if (object instanceof Container container) return VanillaContainerWrapper.of(container);
        return null;
    }
}
