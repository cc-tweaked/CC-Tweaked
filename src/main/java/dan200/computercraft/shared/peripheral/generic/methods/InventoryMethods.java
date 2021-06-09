/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.methods;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.ItemStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.shared.peripheral.generic.methods.ArgumentHelpers.assertBetween;

/**
 * Methods for interacting with inventories.
 *
 * @cc.module inventory
 */
public class InventoryMethods implements GenericSource
{
    @Nonnull
    @Override
    public Identifier id()
    {
        return new Identifier( ComputerCraft.MOD_ID, "inventory" );
    }

    /**
     * Get the size of this inventory.
     *
     * @param inventory The current inventory.
     * @return The number of slots in this inventory.
     */
    @LuaFunction( mainThread = true )
    public static int size( Inventory inventory )
    {
        return extractHandler( inventory ).size();
    }

    /**
     * Get the name of this inventory.
     *
     * @param inventory The current inventory.
     * @return The name of this inventory, or {@code nil} if not present.
     */
    @LuaFunction( mainThread = true )
    public static String name( Inventory inventory )
    {
        if( inventory instanceof Nameable )
        {
            Nameable i = (Nameable) inventory;
            return i.hasCustomName() ? i.getName().asString() : null;
        }
        return null;
    }

    /**
     * List all items in this inventory. This returns a table, with an entry for each slot.
     *
     * Each item in the inventory is represented by a table containing some basic information, much like
     * {@link dan200.computercraft.shared.turtle.apis.TurtleAPI#getItemDetail} includes. More information can be fetched
     * with {@link #getItemDetail}. The table contains the item `name`, the `count` and an a (potentially nil) hash of
     * the item's `nbt.` This NBT data doesn't contain anything useful, but allows you to distinguish identical items.
     *
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
    @LuaFunction( mainThread = true )
    public static Map<Integer, Map<String, ?>> list( Inventory inventory )
    {
        ItemStorage itemStorage = extractHandler( inventory );

        Map<Integer, Map<String, ?>> result = new HashMap<>();
        int size = itemStorage.size();
        for( int i = 0; i < size; i++ )
        {
            ItemStack stack = itemStorage.getStack( i );
            if( !stack.isEmpty() ) result.put( i + 1, ItemData.fillBasic( new HashMap<>( 4 ), stack ) );
        }

        return result;
    }

    /**
     * Get detailed information about an item.
     *
     * The returned information contains the same information as each item in
     * {@link #list}, as well as additional details like the display name
     * (`displayName`) and item durability (`damage`, `maxDamage`, `durability`).
     *
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
     * if item.damage then
     *   print(("Damage: %d/%d"):format(item.damage, item.maxDamage))
     * end
     * }</pre>
     */
    @Nullable
    @LuaFunction( mainThread = true )
    public static Map<String, ?> getItemDetail( Inventory inventory, int slot ) throws LuaException
    {
        ItemStorage itemStorage = extractHandler( inventory );

        assertBetween( slot, 1, itemStorage.size(), "Slot out of range (%s)" );

        ItemStack stack = itemStorage.getStack( slot - 1 );
        return stack.isEmpty() ? null : ItemData.fill( new HashMap<>(), stack );
    }

    /**
     * Get the maximum number of items which can be stored in this slot.
     *
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
     */
    @LuaFunction( mainThread = true )
    public static int getItemLimit( Inventory inventory, int slot ) throws LuaException
    {
        assertBetween( slot, 1, inventory.size(), "Slot out of range (%s)" );
        return inventory.getMaxCountPerStack();
    }

    /**
     * Push items from one inventory to another connected one.
     *
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
    @LuaFunction( mainThread = true )
    public static int pushItems(
        Inventory from, IComputerAccess computer,
        String toName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException
    {
        ItemStorage fromStorage = extractHandler( from );

        // Find location to transfer to
        IPeripheral location = computer.getAvailablePeripheral( toName );
        if( location == null ) throw new LuaException( "Target '" + toName + "' does not exist" );

        ItemStorage toStorage = extractHandler( location.getTarget() );
        if( toStorage == null ) throw new LuaException( "Target '" + toName + "' is not an inventory" );

        // Validate slots
        int actualLimit = limit.orElse( Integer.MAX_VALUE );
        assertBetween( fromSlot, 1, fromStorage.size(), "From slot out of range (%s)" );
        if( toSlot.isPresent() ) assertBetween( toSlot.get(), 1, toStorage.size(), "To slot out of range (%s)" );

        if( actualLimit <= 0 ) return 0;
        return moveItem( fromStorage, fromSlot - 1, toStorage, toSlot.orElse( 0 ) - 1, actualLimit );
    }

    /**
     * Pull items from a connected inventory into this one.
     *
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
    @LuaFunction( mainThread = true )
    public static int pullItems(
        Inventory to, IComputerAccess computer,
        String fromName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException
    {
        // Get appropriate inventory for source peripheral
        ItemStorage toStorage = extractHandler( to );

        // Find location to transfer to
        IPeripheral location = computer.getAvailablePeripheral( fromName );
        if( location == null ) throw new LuaException( "Source '" + fromName + "' does not exist" );

        ItemStorage fromStorage = extractHandler( location.getTarget() );
        if( fromStorage == null ) throw new LuaException( "Source '" + fromName + "' is not an inventory" );

        // Validate slots
        int actualLimit = limit.orElse( Integer.MAX_VALUE );
        assertBetween( fromSlot, 1, fromStorage.size(), "From slot out of range (%s)" );
        if( toSlot.isPresent() ) assertBetween( toSlot.get(), 1, toStorage.size(), "To slot out of range (%s)" );

        if( actualLimit <= 0 ) return 0;
        return moveItem( fromStorage, fromSlot - 1, toStorage, toSlot.orElse( 0 ) - 1, actualLimit );
    }


    @Nullable
    private static ItemStorage extractHandler( @Nullable Object object )
    {
        if( object instanceof BlockEntity )
        {
            Inventory inventory = InventoryUtil.getInventory( (BlockEntity) object );
            if( inventory != null )
            {
                return ItemStorage.wrap( inventory );
            }
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
    private static int moveItem( ItemStorage from, int fromSlot, ItemStorage to, int toSlot, final int limit )
    {
        // Moving nothing is easy
        if( limit == 0 )
        {
            return 0;
        }

        // Get stack to move
        ItemStack stack = InventoryUtil.takeItems( limit, from, fromSlot, 1, fromSlot );
        if( stack.isEmpty() )
        {
            return 0;
        }
        int stackCount = stack.getCount();

        // Move items in
        ItemStack remainder;
        if( toSlot < 0 )
        {
            remainder = InventoryUtil.storeItems( stack, to );
        }
        else
        {
            remainder = InventoryUtil.storeItems( stack, to, toSlot, 1, toSlot );
        }

        // Calculate items moved
        int count = stackCount - remainder.getCount();

        if( !remainder.isEmpty() )
        {
            // Put the remainder back
            InventoryUtil.storeItems( remainder, from, fromSlot, 1, fromSlot );
        }

        return count;
    }
}
