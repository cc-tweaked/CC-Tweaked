// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.PeripheralType;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Methods for interacting with inventories.
 *
 * @param <T> The type for inventories.
 * @cc.module inventory
 * @cc.since 1.94.0
 */
public abstract class AbstractInventoryMethods<T> implements GenericPeripheral {
    @Override
    public final PeripheralType getType() {
        return PeripheralType.ofAdditional("inventory");
    }

    @Override
    public final String id() {
        return ComputerCraftAPI.MOD_ID + ":inventory";
    }

    /**
     * Get the size of this inventory.
     *
     * @param inventory The current inventory.
     * @return The number of slots in this inventory.
     */
    @LuaFunction(mainThread = true)
    public abstract int size(T inventory);

    /**
     * List all items in this inventory. This returns a table, with an entry for each slot.
     * <p>
     * Each item in the inventory is represented by a table containing some basic information, much like
     * {@link dan200.computercraft.shared.turtle.apis.TurtleAPI#getItemDetail(ILuaContext, Optional, Optional)}
     * includes. More information can be fetched with {@link #getItemDetail}. The table contains the item `name`, the
     * `count` and an a (potentially nil) hash of the item's `nbt.` This NBT data doesn't contain anything useful, but
     * allows you to distinguish identical items.
     * <p>
     * The returned table is sparse, and so empty slots will be `nil` - it is recommended to loop over using [`pairs`]
     * rather than [`ipairs`].
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
    public abstract Map<Integer, Map<String, ?>> list(T inventory);

    /**
     * Get detailed information about an item.
     * <p>
     * The returned information contains the same information as each item in
     * {@link #list}, as well as additional details like the display name
     * (`displayName`), and item and item durability (`damage`, `maxDamage`, `durability`).
     * <p>
     * Some items include more information (such as enchantments) - it is
     * recommended to print it out using [`textutils.serialize`] or in the Lua
     * REPL, to explore what is available.
     * <p>
     * > [Deprecated fields][!INFO]
     * > Older versions of CC: Tweaked exposed an {@code itemGroups} field, listing the
     * > creative tabs an item was available under. This information is no longer available on
     * > more recent versions of the game, and so this field will always be empty. Do not use this
     * > field in new code!
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
     * if item.damage then
     *   print(("Damage: %d/%d"):format(item.damage, item.maxDamage))
     * end
     * }</pre>
     */
    @Nullable
    @LuaFunction(mainThread = true)
    public abstract Map<String, ?> getItemDetail(T inventory, int slot) throws LuaException;

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
    public abstract long getItemLimit(T inventory, int slot) throws LuaException;

    /**
     * Push items from one inventory to another connected one.
     * <p>
     * This allows you to push an item in an inventory to another inventory <em>on the same wired network</em>. Both
     * inventories must attached to wired modems which are connected via a cable.
     *
     * @param from     Inventory to move items from.
     * @param computer The current computer.
     * @param toName   The name of the peripheral/inventory to push to. This is the string given to [`peripheral.wrap`],
     *                 and displayed by the wired modem.
     * @param fromSlot The slot in the current inventory to move items to.
     * @param limit    The maximum number of items to move. Defaults to the current stack limit.
     * @param toSlot   The slot in the target inventory to move to. If not given, the item will be inserted into any slot.
     * @return The number of transferred items.
     * @throws LuaException If the peripheral to transfer to doesn't exist or isn't an inventory.
     * @throws LuaException If either source or destination slot is out of range.
     * @cc.see peripheral.getName Allows you to get the name of a [wrapped][`peripheral.wrap`] peripheral.
     * @cc.usage Wrap two chests, and push an item from one to another.
     * <pre>{@code
     * local chest_a = peripheral.wrap("minecraft:chest_0")
     * local chest_b = peripheral.wrap("minecraft:chest_1")
     *
     * chest_a.pushItems(peripheral.getName(chest_b), 1)
     * }</pre>
     */
    @LuaFunction(mainThread = true)
    public abstract int pushItems(
        T from, IComputerAccess computer, String toName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException;

    /**
     * Pull items from a connected inventory into this one.
     * <p>
     * This allows you to transfer items between inventories <em>on the same wired network</em>. Both this and the source
     * inventory must attached to wired modems which are connected via a cable.
     *
     * @param to       Inventory to move items to.
     * @param computer The current computer.
     * @param fromName The name of the peripheral/inventory to pull from. This is the string given to [`peripheral.wrap`],
     *                 and displayed by the wired modem.
     * @param fromSlot The slot in the source inventory to move items from.
     * @param limit    The maximum number of items to move. Defaults to the current stack limit.
     * @param toSlot   The slot in current inventory to move to. If not given, the item will be inserted into any slot.
     * @return The number of transferred items.
     * @throws LuaException If the peripheral to transfer to doesn't exist or isn't an inventory.
     * @throws LuaException If either source or destination slot is out of range.
     * @cc.see peripheral.getName Allows you to get the name of a [wrapped][`peripheral.wrap`] peripheral.
     * @cc.usage Wrap two chests, and push an item from one to another.
     * <pre>{@code
     * local chest_a = peripheral.wrap("minecraft:chest_0")
     * local chest_b = peripheral.wrap("minecraft:chest_1")
     *
     * chest_a.pullItems(peripheral.getName(chest_b), 1)
     * }</pre>
     */
    @LuaFunction(mainThread = true)
    public abstract int pullItems(
        T to, IComputerAccess computer, String fromName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException;
}
