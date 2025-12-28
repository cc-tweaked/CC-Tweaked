// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic.methods;

import com.google.common.annotations.VisibleForTesting;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.ForgeContainerTransfer;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.core.util.ArgumentHelpers.assertBetween;

/**
 * Inventory methods for Forge's {@link ResourceHandler}.
 */
public final class InventoryMethods extends AbstractInventoryMethods<InventoryMethods.StorageWrapper> {
    private final HolderLookup.Provider registries;

    public InventoryMethods(MinecraftServer registries) {
        this.registries = registries.registryAccess();
    }

    @VisibleForTesting
    InventoryMethods(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    public record StorageWrapper(ResourceHandler<ItemResource> storage) {
    }

    @Override
    @LuaFunction(mainThread = true)
    public int size(StorageWrapper inventory) {
        return inventory.storage().size();
    }

    @Override
    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, ?>> list(StorageWrapper wrapper) {
        var storage = wrapper.storage();
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        var size = storage.size();
        for (var i = 0; i < size; i++) {
            var stack = storage.getResource(i).toStack(storage.getAmountAsInt(i));
            if (!stack.isEmpty()) {
                result.put(i + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(registries, stack));
            }
        }

        return result;
    }

    @Override
    @Nullable
    @LuaFunction(mainThread = true)
    public Map<String, ?> getItemDetail(StorageWrapper wrapper, int slot) throws LuaException {
        var storage = wrapper.storage();
        assertBetween(slot, 1, storage.size(), "Slot out of range (%s)");

        var stack = storage.getResource(slot - 1).toStack(storage.getAmountAsInt(slot - 1));
        return stack.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(registries, stack);
    }

    @Override
    @LuaFunction(mainThread = true)
    public long getItemLimit(StorageWrapper wrapper, int slot) throws LuaException {
        var storage = wrapper.storage();
        assertBetween(slot, 1, storage.size(), "Slot out of range (%s)");

        // FIXME: The capacity will be 0 if the resource is empty (or not valid). If empty, we try with dirt.
        var item = storage.getResource(slot - 1);
        return storage.getCapacityAsLong(slot - 1, item.isEmpty() ? ItemResource.of(Items.DIRT) : item);
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

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, from.storage().size(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, to.size(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(from.storage(), fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
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

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, from.size(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, to.storage().size(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(from, fromSlot - 1, to.storage(), toSlot.orElse(0) - 1, actualLimit);
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
    private static int moveItem(ResourceHandler<ItemResource> from, int fromSlot, ResourceHandler<ItemResource> to, int toSlot, final int limit) {
        var fromWrapper = new ForgeContainerTransfer(from).singleSlot(fromSlot);
        var toWrapper = new ForgeContainerTransfer(to);
        if (toSlot >= 0) toWrapper = toWrapper.singleSlot(toSlot);

        return Math.max(0, fromWrapper.moveTo(toWrapper, limit));
    }
}
