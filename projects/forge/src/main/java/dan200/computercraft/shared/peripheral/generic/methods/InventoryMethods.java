/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.shared.platform.ForgeContainerTransfer;
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

import static dan200.computercraft.shared.util.ArgumentHelpers.assertBetween;

/**
 * Methods for interacting with inventories.
 *
 * @cc.module inventory
 * @cc.since 1.94.0
 */
public class InventoryMethods implements GenericPeripheral {
    @Override
    public PeripheralType getType() {
        return PeripheralType.ofAdditional("inventory");
    }

    @Override
    public String id() {
        return ComputerCraftAPI.MOD_ID + ":inventory";
    }

    /**
     * Get the size of this inventory.
     *
     * @param inventory The current inventory.
     * @return The number of slots in this inventory.
     */
    @LuaFunction(mainThread = true)
    public static int size(IItemHandler inventory) {
        return inventory.getSlots();
    }

    /**
     * List all items in this inventory. This returns a table, with an entry for each slot.
     * <p>
     * Each item in the inventory is represented by a table containing some basic information, much like
     * {@link dan200.computercraft.shared.turtle.apis.TurtleAPI#getItemDetail(ILuaContext, Optional, Optional)}
     * includes. More information can be fetched with {@link #getItemDetail}. The table contains the item `name`, the
     * `count` and an a (potentially nil) hash of the item's `nbt.` This NBT data doesn't contain anything useful, but
     * allows you to distinguish identical items.
     * <p>
     * The returned table is sparse, and so empty slots will be `nil` - it is recommended to loop over using `pairs`
     * rather than `ipairs`.
     *
     * @param inventory The current inventory.
     * @return All items in this inventory.
     * @cc.treturn { (table|nil)... } All items in this inventory.
     * @cc.usage Find an adjacent chest and print all items in it.
     *
     * <pre>{@code
     * local chest = peripheral.find("minecraft:chest")
     * for slot, item in pairs(chest.list()) do
     *   print(("%d x %s in slot %d"):format(item.count, item.name, slot))
     * end
     * }</pre>
     */
    @LuaFunction(mainThread = true)
    public static Map<Integer, Map<String, ?>> list(IItemHandler inventory) {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        var size = inventory.getSlots();
        for (var i = 0; i < size; i++) {
            var stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) result.put(i + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
        }

        return result;
    }

    /**
     * Get detailed information about an item.
     * <p>
     * The returned information contains the same information as each item in
     * {@link #list}, as well as additional details like the display name
     * (`displayName`), item groups (`itemGroups`), which are the creative tabs
     * an item will appear under, and item and item durability (`damage`,
     * `maxDamage`, `durability`).
     * <p>
     * Some items include more information (such as enchantments) - it is
     * recommended to print it out using @{textutils.serialize} or in the Lua
     * REPL, to explore what is available.
     *
     * @param inventory The current inventory.
     * @param slot      The slot to get information about.
     * @return Information about the item in this slot, or {@code nil} if not present.
     * @throws LuaException If the slot is out of range.
     * @cc.treturn table Information about the item in this slot, or {@code nil} if not present.
     * @cc.usage Print some information about the first in a chest.
     *
     * <pre>{@code
     * local chest = peripheral.find("minecraft:chest")
     * local item = chest.getItemDetail(1)
     * if not item then print("No item") return end
     *
     * print(("%s (%s)"):format(item.displayName, item.name))
     * print(("Count: %d/%d"):format(item.count, item.maxCount))
     *
     * for _, group in pairs(item.itemGroups) do
     *   print(("Group: %s"):format(group.displayName))
     * end
     *
     * if item.damage then
     *   print(("Damage: %d/%d"):format(item.damage, item.maxDamage))
     * end
     * }</pre>
     */
    @Nullable
    @LuaFunction(mainThread = true)
    public static Map<String, ?> getItemDetail(IItemHandler inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.getSlots(), "Slot out of range (%s)");

        var stack = inventory.getStackInSlot(slot - 1);
        return stack.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(stack);
    }

    /**
     * Get the maximum number of items which can be stored in this slot.
     * <p>
     * Typically this will be limited to 64 items. However, some inventories (such as barrels or caches) can store
     * hundreds or thousands of items in one slot.
     *
     * @param inventory Inventory to probe.
     * @param slot      The slot
     * @return The maximum number of items in this slot.
     * @throws LuaException If the slot is out of range.
     * @cc.usage Count the maximum number of items an adjacent chest can hold.
     * <pre>{@code
     * local chest = peripheral.find("minecraft:chest")
     * local total = 0
     * for i = 1, chest.size() do
     *   total = total + chest.getItemLimit(i)
     * end
     * print(total)
     * }</pre>
     * @cc.since 1.96.0
     */
    @LuaFunction(mainThread = true)
    public static int getItemLimit(IItemHandler inventory, int slot) throws LuaException {
        assertBetween(slot, 1, inventory.getSlots(), "Slot out of range (%s)");
        return inventory.getSlotLimit(slot - 1);
    }

    /**
     * Push items from one inventory to another connected one.
     * <p>
     * This allows you to push an item in an inventory to another inventory <em>on the same wired network</em>. Both
     * inventories must attached to wired modems which are connected via a cable.
     *
     * @param from     Inventory to move items from.
     * @param computer The current computer.
     * @param toName   The name of the peripheral/inventory to push to. This is the string given to @{peripheral.wrap},
     *                 and displayed by the wired modem.
     * @param fromSlot The slot in the current inventory to move items to.
     * @param limit    The maximum number of items to move. Defaults to the current stack limit.
     * @param toSlot   The slot in the target inventory to move to. If not given, the item will be inserted into any slot.
     * @return The number of transferred items.
     * @throws LuaException If the peripheral to transfer to doesn't exist or isn't an inventory.
     * @throws LuaException If either source or destination slot is out of range.
     * @cc.see peripheral.getName Allows you to get the name of a @{peripheral.wrap|wrapped} peripheral.
     * @cc.usage Wrap two chests, and push an item from one to another.
     * <pre>{@code
     * local chest_a = peripheral.wrap("minecraft:chest_0")
     * local chest_b = peripheral.wrap("minecraft:chest_1")
     *
     * chest_a.pushItems(peripheral.getName(chest_b), 1)
     * }</pre>
     */
    @LuaFunction(mainThread = true)
    public static int pushItems(
        IItemHandler from, IComputerAccess computer,
        String toName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException {
        // Find location to transfer to
        var location = computer.getAvailablePeripheral(toName);
        if (location == null) throw new LuaException("Target '" + toName + "' does not exist");

        var to = extractHandler(location.getTarget());
        if (to == null) throw new LuaException("Target '" + toName + "' is not an inventory");

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, from.getSlots(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, to.getSlots(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(from, fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
    }

    /**
     * Pull items from a connected inventory into this one.
     * <p>
     * This allows you to transfer items between inventories <em>on the same wired network</em>. Both this and the source
     * inventory must attached to wired modems which are connected via a cable.
     *
     * @param to       Inventory to move items to.
     * @param computer The current computer.
     * @param fromName The name of the peripheral/inventory to pull from. This is the string given to @{peripheral.wrap},
     *                 and displayed by the wired modem.
     * @param fromSlot The slot in the source inventory to move items from.
     * @param limit    The maximum number of items to move. Defaults to the current stack limit.
     * @param toSlot   The slot in current inventory to move to. If not given, the item will be inserted into any slot.
     * @return The number of transferred items.
     * @throws LuaException If the peripheral to transfer to doesn't exist or isn't an inventory.
     * @throws LuaException If either source or destination slot is out of range.
     * @cc.see peripheral.getName Allows you to get the name of a @{peripheral.wrap|wrapped} peripheral.
     * @cc.usage Wrap two chests, and push an item from one to another.
     * <pre>{@code
     * local chest_a = peripheral.wrap("minecraft:chest_0")
     * local chest_b = peripheral.wrap("minecraft:chest_1")
     *
     * chest_a.pullItems(peripheral.getName(chest_b), 1)
     * }</pre>
     */
    @LuaFunction(mainThread = true)
    public static int pullItems(
        IItemHandler to, IComputerAccess computer,
        String fromName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException {
        // Find location to transfer to
        var location = computer.getAvailablePeripheral(fromName);
        if (location == null) throw new LuaException("Source '" + fromName + "' does not exist");

        var from = extractHandler(location.getTarget());
        if (from == null) throw new LuaException("Source '" + fromName + "' is not an inventory");

        // Validate slots
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        assertBetween(fromSlot, 1, from.getSlots(), "From slot out of range (%s)");
        if (toSlot.isPresent()) assertBetween(toSlot.get(), 1, to.getSlots(), "To slot out of range (%s)");

        if (actualLimit <= 0) return 0;
        return moveItem(from, fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
    }

    @Nullable
    private static IItemHandler extractHandler(@Nullable Object object) {
        if (object instanceof BlockEntity blockEntity && blockEntity.isRemoved()) return null;

        if (object instanceof ICapabilityProvider provider) {
            var cap = provider.getCapability(ForgeCapabilities.ITEM_HANDLER);
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
