// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.ForgeContainerTransfer;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.core.util.ArgumentHelpers.assertBetween;

/**
 * Inventory methods for Forge's {@link IItemHandler}.
 */
public final class InventoryMethods extends AbstractInventoryMethods<IItemHandler> {
    @Override
    @LuaFunction(mainThread = true)
    public int size(IItemHandler inventory) {
        return inventory.getSlots();
    }

    @Override
    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, ?>> list(IItemHandler inventory) {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        var size = inventory.getSlots();
        for (var i = 0; i < size; i++) {
            var stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) result.put(i + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
        }

        return result;
    }

    @Override
    @Nullable
    @LuaFunction(mainThread = true)
    public Map<String, ?> getItemDetail(IItemHandler inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.getSlots(), "Slot out of range (%s)");

        var stack = inventory.getStackInSlot(slot - 1);
        return stack.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(stack);
    }

    @Override
    @LuaFunction(mainThread = true)
    public long getItemLimit(IItemHandler inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.getSlots(), "Slot out of range (%s)");
        return inventory.getSlotLimit(slot - 1);
    }

    @Override
    @LuaFunction(mainThread = true)
    public int pushItems(
        IItemHandler from, IComputerAccess computer,
        String toName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException {
        // Find location to transfer to
        var location = computer.getAvailablePeripheral(toName);
        if (location == null) throw new LuaException("Target '" + toName + "' does not exist");

        var to = extractHandler(location);
        if (to == null) throw new LuaException("Target '" + toName + "' is not an inventory");

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, from.getSlots(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, to.getSlots(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(from, fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
    }

    @Override
    @LuaFunction(mainThread = true)
    public int pullItems(
        IItemHandler to, IComputerAccess computer,
        String fromName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException {
        // Find location to transfer to
        var location = computer.getAvailablePeripheral(fromName);
        if (location == null) throw new LuaException("Source '" + fromName + "' does not exist");

        var from = extractHandler(location);
        if (from == null) throw new LuaException("Source '" + fromName + "' is not an inventory");

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, from.getSlots(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, to.getSlots(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(from, fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
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
    private static int moveItem(IItemHandler from, int fromSlot, IItemHandler to, int toSlot, final int limit) {
        var fromWrapper = new ForgeContainerTransfer(from).singleSlot(fromSlot);
        var toWrapper = new ForgeContainerTransfer(to);
        if (toSlot >= 0) toWrapper = toWrapper.singleSlot(toSlot);

        return Math.max(0, fromWrapper.moveTo(toWrapper, limit));
    }
}
