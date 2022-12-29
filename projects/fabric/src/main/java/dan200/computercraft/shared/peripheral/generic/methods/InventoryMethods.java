/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.generic.SidedGenericPeripheral;
import dan200.computercraft.shared.platform.FabricContainerTransfer;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.shared.util.ArgumentHelpers.assertBetween;

/**
 * Methods for interacting with inventories. This mirrors the Forge version.
 */
@SuppressWarnings("UnstableApiUsage")
public class InventoryMethods implements GenericPeripheral {
    @Override
    public PeripheralType getType() {
        return PeripheralType.ofAdditional("inventory");
    }

    @Override
    public String id() {
        return ComputerCraftAPI.MOD_ID + ":inventory";
    }

    @LuaFunction(mainThread = true)
    public static int size(InventoryStorage inventory) {
        return inventory.getSlots().size();
    }

    @LuaFunction(mainThread = true)
    public static Map<Integer, Map<String, ?>> list(InventoryStorage inventory) {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        var slots = inventory.getSlots();
        var size = slots.size();
        for (var i = 0; i < size; i++) {
            var stack = toStack(slots.get(i));
            if (!stack.isEmpty()) result.put(i + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
        }

        return result;
    }

    @Nullable
    @LuaFunction(mainThread = true)
    public static Map<String, ?> getItemDetail(InventoryStorage inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.getSlots().size(), "Slot out of range (%s)");

        var stack = toStack(inventory.getSlot(slot - 1));
        return stack.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(stack);
    }

    @LuaFunction(mainThread = true)
    public static long getItemLimit(InventoryStorage inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.getSlots().size(), "Slot out of range (%s)");
        return inventory.getSlot(slot - 1).getCapacity();
    }

    @LuaFunction(mainThread = true)
    public static int pushItems(
        InventoryStorage from, IComputerAccess computer,
        String toName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException {
        // Find location to transfer to
        var location = computer.getAvailablePeripheral(toName);
        if (location == null) throw new LuaException("Target '" + toName + "' does not exist");

        var to = extractHandler(location);
        if (to == null) throw new LuaException("Target '" + toName + "' is not an inventory");

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, from.getSlots().size(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, to.getSlots().size(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(from, fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
    }

    @LuaFunction(mainThread = true)
    public static int pullItems(
        InventoryStorage to, IComputerAccess computer,
        String fromName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException {
        // Find location to transfer to
        var location = computer.getAvailablePeripheral(fromName);
        if (location == null) throw new LuaException("Source '" + fromName + "' does not exist");

        var from = extractHandler(location);
        if (from == null) throw new LuaException("Source '" + fromName + "' is not an inventory");

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, from.getSlots().size(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, to.getSlots().size(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(from, fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
    }

    public static @Nullable Container extractContainer(Container container) {
        return container instanceof ChestBlockEntity chest && chest.getBlockState().getBlock() instanceof ChestBlock chestBlock
            ? ChestBlock.getContainer(chestBlock, chest.getBlockState(), chest.getLevel(), chest.getBlockPos(), true)
            : container;
    }

    @Nullable
    private static InventoryStorage extractHandler(IPeripheral peripheral) {
        var object = peripheral.getTarget();
        var direction = peripheral instanceof SidedGenericPeripheral sided ? sided.direction() : null;

        if (object instanceof BlockEntity blockEntity && blockEntity.isRemoved()) return null;

        if (object instanceof InventoryStorage storage) return storage;
        if (object instanceof Container container && (container = extractContainer(container)) != null) {
            return InventoryStorage.of(container, null);
        }

        if (object instanceof BlockEntity blockEntity && direction != null) {
            var found = ItemStorage.SIDED.find(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            if (found instanceof InventoryStorage storage) return storage;
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
    private static int moveItem(InventoryStorage from, int fromSlot, InventoryStorage to, int toSlot, final int limit) {
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
