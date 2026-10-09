// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.FabricItemContainer;
import dan200.computercraft.shared.platform.ItemContainer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Inventory methods for Fabric's {@link SlottedStorage} and {@link ItemVariant}s.
 * <p>
 * The generic peripheral system doesn't (currently) support generics, and so we need to wrap this in a
 * {@link StorageWrapper} box.
 */
@SuppressWarnings("UnstableApiUsage")
public final class InventoryMethods extends AbstractInventoryMethods<InventoryMethods.StorageWrapper> {
    /**
     * Wrapper over a {@link SlottedStorage}.
     */
    public static final class StorageWrapper {
        private final Storage<ItemVariant> storage;
        private final FabricItemContainer container;

        public StorageWrapper(Storage<ItemVariant> storage) {
            this.storage = storage;
            this.container = new FabricItemContainer(storage);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof StorageWrapper other)) return false;

            var otherStorage = other.storage;

            /*
             Equality for inventory storage isn't really defined, and most of the time falls back to reference
             equality.
              - Vanilla inventories are exposed via InventoryStorage - the creation of this is cached, so will be
                the same object.
              - Double chests are combined into a CombinedSlottedStorage. We check the parts are equal.
            */
            if (
                storage instanceof CombinedSlottedStorage<?, ?> cs && storage.getClass() == otherStorage.getClass()
                    && cs.parts.equals(((CombinedStorage<?, ?>) otherStorage).parts)
            ) {
                return true;
            }

            return storage.equals(otherStorage);
        }

        @Override
        public int hashCode() {
            return storage instanceof CombinedSlottedStorage<?, ?> cs ? cs.parts.hashCode() : storage.hashCode();
        }
    }

    @Override
    protected ItemContainer getContainer(StorageWrapper inventory) {
        return inventory.container;
    }

    @Override
    protected @Nullable ItemContainer getContainer(IPeripheral peripheral) {
        var object = peripheral.getTarget();
        var direction = peripheral instanceof dan200.computercraft.shared.peripheral.generic.GenericPeripheral sided ? sided.side() : null;

        if (object instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved()) return null;

            var found = extractContainerImpl(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            if (found != null) return new FabricItemContainer(found);
        }

        return null;
    }

    public static @Nullable StorageWrapper extractContainer(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Direction direction) {
        var storage = extractContainerImpl(level, pos, state, blockEntity, direction);
        return storage == null ? null : new StorageWrapper(storage);
    }

    @SuppressWarnings("NullAway") // FIXME: Doesn't cope with @Nullable type parameter.
    private static @Nullable Storage<ItemVariant> extractContainerImpl(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Direction direction) {
        var internal = ItemStorage.SIDED.find(level, pos, state, blockEntity, null);
        if (internal != null) return internal;

        return direction != null ? ItemStorage.SIDED.find(level, pos, state, blockEntity, direction) : null;
    }
}
