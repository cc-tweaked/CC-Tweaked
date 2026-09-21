// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import com.google.common.collect.Iterables;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.FabricContainerTransfer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.core.util.ArgumentHelpers.assertBetween;

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
     *
     */
    public static final class StorageWrapper {
        private final Storage<ItemVariant> storage;
        private final @Nullable SlottedStorage<ItemVariant> slottedStorage;

        public StorageWrapper(Storage<ItemVariant> storage) {
            this.storage = storage;
            this.slottedStorage = storage instanceof SlottedStorage<ItemVariant> slotted ? slotted : null;
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

        private int size() {
            return slottedStorage != null ? slottedStorage.getSlotCount() : Iterables.size(storage);
        }

        private StorageView<ItemVariant> getSlot(int slot) {
            if (slottedStorage != null) return slottedStorage.getSlot(slot);

            var singleSlot = Iterables.get(storage, slot);
            if (singleSlot == null) throw new IndexOutOfBoundsException();
            return singleSlot;
        }
    }

    @Override
    @LuaFunction(mainThread = true)
    public int size(StorageWrapper inventory) {
        return inventory.size();
    }

    @Override
    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, ?>> list(StorageWrapper inventory) {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        var i = 0;
        for (var slots = inventory.storage.iterator(); slots.hasNext(); i++) {
            var stack = toStack(slots.next());
            if (!stack.isEmpty()) result.put(i + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
        }

        return result;
    }

    @Override
    @Nullable
    @LuaFunction(mainThread = true)
    public Map<String, ?> getItemDetail(StorageWrapper inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.size(), "Slot out of range (%s)");

        var stack = toStack(inventory.getSlot(slot - 1));
        return stack.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(stack);
    }

    @Override
    @LuaFunction(mainThread = true)
    public long getItemLimit(StorageWrapper inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.size(), "Slot out of range (%s)");
        return inventory.getSlot(slot - 1).getCapacity();
    }

    @Override
    @LuaFunction(mainThread = true)
    public int pushItems(
        StorageWrapper from, IComputerAccess computer,
        String toName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException {
        // Find location to transfer to
        var location = computer.getAvailablePeripheral(toName);
        if (location == null) throw new LuaException("Target '" + toName + "' does not exist");

        var to = extractHandler(location);
        if (to == null) throw new LuaException("Target '" + toName + "' is not an inventory");

        return moveImpl(from, fromSlot, to, toSlot, limit);
    }

    @Override
    @LuaFunction(mainThread = true)
    public int pullItems(
        StorageWrapper to, IComputerAccess computer,
        String fromName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException {
        // Find location to transfer to
        var location = computer.getAvailablePeripheral(fromName);
        if (location == null) throw new LuaException("Source '" + fromName + "' does not exist");

        var from = extractHandler(location);
        if (from == null) throw new LuaException("Source '" + fromName + "' is not an inventory");

        return moveImpl(from, fromSlot, to, toSlot, limit);
    }

    private static int moveImpl(StorageWrapper from, int fromSlot, StorageWrapper to, Optional<Integer> toSlot, Optional<Integer> limit) throws LuaException {
        assertBetween(fromSlot, 1, from.size(), "From slot out of range (%s)");
        var fromStorage = from.getSlot(fromSlot - 1);

        Storage<ItemVariant> toStorage;
        if (toSlot.isPresent()) {
            if (to.slottedStorage == null) throw new LuaException("Cannot specify toSlot for this inventory");
            assertBetween(toSlot.get(), 1, to.size(), "To slot out of range (%s)");
            toStorage = to.slottedStorage.getSlot(toSlot.get() - 1);
        } else {
            toStorage = to.storage;
        }

        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        if (actualLimit <= 0) return 0;

        return Math.max(0, FabricContainerTransfer.move(fromStorage, toStorage, actualLimit));
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

    @Nullable
    private static StorageWrapper extractHandler(IPeripheral peripheral) {
        var object = peripheral.getTarget();
        var direction = peripheral instanceof dan200.computercraft.shared.peripheral.generic.GenericPeripheral sided ? sided.side() : null;

        if (object instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved()) return null;

            var found = extractContainer(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            if (found != null) return found;
        }

        return null;
    }

    private static ItemStack toStack(StorageView<ItemVariant> variant) {
        if (variant.isResourceBlank() || variant.getAmount() <= 0) return ItemStack.EMPTY;
        return toStack(variant.getResource(), variant.getAmount());
    }

    private static ItemStack toStack(ItemVariant variant, long amount) {
        // I don't care about supporting this much power creep :D
        return variant.toStack(amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount);
    }
}
