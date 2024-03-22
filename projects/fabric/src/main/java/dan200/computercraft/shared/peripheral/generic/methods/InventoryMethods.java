// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.FabricContainerTransfer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
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
     * @param storage The underlying storage
     */
    public record StorageWrapper(SlottedStorage<ItemVariant> storage) {
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
    @LuaFunction(mainThread = true)
    public int size(StorageWrapper inventory) {
        return inventory.storage().getSlots().size();
    }

    @Override
    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, ?>> list(StorageWrapper inventory) {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        var slots = inventory.storage().getSlots();
        var size = slots.size();
        for (var i = 0; i < size; i++) {
            var stack = toStack(slots.get(i));
            if (!stack.isEmpty()) result.put(i + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
        }

        return result;
    }

    @Override
    @Nullable
    @LuaFunction(mainThread = true)
    public Map<String, ?> getItemDetail(StorageWrapper inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.storage().getSlotCount(), "Slot out of range (%s)");

        var stack = toStack(inventory.storage().getSlot(slot - 1));
        return stack.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(stack);
    }

    @Override
    @LuaFunction(mainThread = true)
    public long getItemLimit(StorageWrapper inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.storage().getSlotCount(), "Slot out of range (%s)");
        return inventory.storage().getSlot(slot - 1).getCapacity();
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

        var fromStorage = from.storage();

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, fromStorage.getSlotCount(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, to.getSlots().size(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(fromStorage, fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
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

        var toStorage = to.storage();

        var from = extractHandler(location);
        if (from == null) throw new LuaException("Source '" + fromName + "' is not an inventory");

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, from.getSlots().size(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, toStorage.getSlotCount(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(from, fromSlot - 1, toStorage, toSlot.orElse(0) - 1, actualLimit);
    }

    public static @Nullable StorageWrapper extractContainer(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Direction direction) {
        var storage = extractContainerImpl(level, pos, state, blockEntity, direction);
        return storage == null ? null : new StorageWrapper(storage);
    }

    @SuppressWarnings("NullAway") // FIXME: Doesn't cope with @Nullable type parameter.
    private static @Nullable SlottedStorage<ItemVariant> extractContainerImpl(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Direction direction) {
        var internal = ItemStorage.SIDED.find(level, pos, state, blockEntity, null);
        if (internal instanceof SlottedStorage<ItemVariant> storage) return storage;

        if (direction != null) {
            var external = ItemStorage.SIDED.find(level, pos, state, blockEntity, direction);
            if (external instanceof SlottedStorage<ItemVariant> storage) return storage;
        }

        return null;
    }

    @Nullable
    private static SlottedStorage<ItemVariant> extractHandler(IPeripheral peripheral) {
        var object = peripheral.getTarget();
        var direction = peripheral instanceof dan200.computercraft.shared.peripheral.generic.GenericPeripheral sided ? sided.side() : null;

        if (object instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved()) return null;

            var found = extractContainerImpl(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            if (found != null) return found;
        }

        return null;
    }

    /**
     * Move an item from one handler to another.
     *
     * @param from     The handler to move from.
     * @param fromSlot The slot to move from.
     * @param to       The handler to move to.
     * @param toSlot   The slot to move to. Use any number < 0 to represent any slot.
     * @param limit    The max number to move. {@link Integer#MAX_VALUE} for no limit.
     * @return The number of items moved.
     */
    private static int moveItem(SlottedStorage<ItemVariant> from, int fromSlot, SlottedStorage<ItemVariant> to, int toSlot, final int limit) {
        var fromWrapper = FabricContainerTransfer.of(from).singleSlot(fromSlot);
        var toWrapper = FabricContainerTransfer.of(to);

        return Math.max(0, fromWrapper.moveTo(toSlot >= 0 ? toWrapper.singleSlot(toSlot) : toWrapper, limit));
    }

    private static ItemStack toStack(SingleSlotStorage<ItemVariant> variant) {
        if (variant.isResourceBlank() || variant.getAmount() <= 0) return ItemStack.EMPTY;
        return toStack(variant.getResource(), variant.getAmount());
    }

    private static ItemStack toStack(ItemVariant variant, long amount) {
        // I don't care about supporting this much power creep :D
        return variant.toStack(amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount);
    }
}
