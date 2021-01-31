/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.methods;

import com.google.auto.service.AutoService;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.asm.GenericSource;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
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
@AutoService( GenericSource.class )
public class InventoryMethods implements GenericSource
{
    @Nonnull
    @Override
    public Identifier id()
    {
        return new Identifier(ComputerCraft.MOD_ID, "inventory" );
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
        return inventory.size();
    }

    /**
     * Get the name of this inventory.
     *
     * @param inventory The current inventory.
     * @return The name of this inventory, or {@code nil} if not present.
     */
    @LuaFunction( mainThread = true )
    public static String name( Nameable inventory )
    {
        return inventory.hasCustomName() ? inventory.getName().asString() : null;
    }

    /**
     * List all items in this inventory. This returns a table, with an entry for each slot.
     *
     * Each item in the inventory is represented by a table containing some basic information, much like
     * @link dan200.computercraft.shared.turtle.apis.TurtleAPI#getItemDetail includes. More information can be fetched
     * with {@link #getItemDetail}.
     *
     * The table is sparse, and so empty slots will be `nil` - it is recommended to loop over using `pairs` rather than
     * `ipairs`.
     *
     * @param inventory The current inventory.
     * @return All items in this inventory.
     * @cc.treturn { (table|nil)... } All items in this inventory.
     */
    @LuaFunction( mainThread = true )
    public static Map<Integer, Map<String, ?>> list( Inventory inventory )
    {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        int size = inventory.size();
        for( int i = 0; i < size; i++ )
        {
            ItemStack stack = inventory.getStack( i );
            if( !stack.isEmpty() ) result.put( i + 1, ItemData.fillBasic( new HashMap<>( 4 ), stack ) );
        }

        return result;
    }

    /**
     * Get detailed information about an item.
     *
     * @param inventory The current inventory.
     * @param slot      The slot to get information about.
     * @return Information about the item in this slot, or {@code nil} if not present.
     * @throws LuaException If the slot is out of range.
     * @cc.treturn table Information about the item in this slot, or {@code nil} if not present.
     */
    @Nullable
    @LuaFunction( mainThread = true )
    public static Map<String, ?> getItemDetail( Inventory inventory, int slot ) throws LuaException
    {
        assertBetween( slot, 1, inventory.size(), "Slot out of range (%s)" );

        ItemStack stack = inventory.getStack( slot - 1 );
        return stack.isEmpty() ? null : ItemData.fill( new HashMap<>(), stack );
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
        // Find location to transfer to
        IPeripheral location = computer.getAvailablePeripheral( toName );
        if( location == null ) throw new LuaException( "Target '" + toName + "' does not exist" );

        Inventory to = extractHandler( location.getTarget() );
        if( to == null ) throw new LuaException( "Target '" + toName + "' is not an inventory" );

        // Validate slots
        int actualLimit = limit.orElse( Integer.MAX_VALUE );
        assertBetween( fromSlot, 1, from.size(), "From slot out of range (%s)" );
        if( toSlot.isPresent() ) assertBetween( toSlot.get(), 1, to.size(), "To slot out of range (%s)" );

        if( actualLimit <= 0 ) return 0;
        return moveItem( from, fromSlot - 1, to, toSlot.orElse( 0 ) - 1, actualLimit );
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
        // Find location to transfer to
        IPeripheral location = computer.getAvailablePeripheral( fromName );
        if( location == null ) throw new LuaException( "Source '" + fromName + "' does not exist" );

        Inventory from = extractHandler( location.getTarget() );
        if( from == null ) throw new LuaException( "Source '" + fromName + "' is not an inventory" );

        // Validate slots
        int actualLimit = limit.orElse( Integer.MAX_VALUE );
        assertBetween( fromSlot, 1, from.size(), "From slot out of range (%s)" );
        if( toSlot.isPresent() ) assertBetween( toSlot.get(), 1, to.size(), "To slot out of range (%s)" );

        if( actualLimit <= 0 ) return 0;
        return moveItem( from, fromSlot - 1, to, toSlot.orElse( 0 ) - 1, actualLimit );
    }
    
    @Nullable
    private static Inventory extractHandler( @Nullable Object object )
    {
        if ( object instanceof Inventory ) return (Inventory) object;
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
    private static int moveItem( Inventory from, int fromSlot, Inventory to, int toSlot, final int limit )
    {
        // See how much we can get out of this slot
        // ItemStack extracted = from.extractItem( fromSlot, limit, true );
        // if( extracted.isEmpty() ) return 0;

        // Limit the amount to extract
        // int extractCount = Math.min( extracted.getCount(), limit );
        // extracted.setCount( extractCount );

        // ItemStack remainder = toSlot < 0 ? ItemHandlerHelper.insertItem( to, extracted, false ) : to.insertItem( toSlot, extracted, false );
        // int inserted = remainder.isEmpty() ? extractCount : extractCount - remainder.getCount();
        // if( inserted <= 0 ) return 0;

        // Remove the item from the original inventory. Technically this could fail, but there's little we can do
        // about that.
        //from.extractItem( fromSlot, inserted, false );

        Boolean recurse = false;

        ItemStack source = from.getStack( fromSlot );
        int count = 0;

        if (toSlot > 0) {
            int space = amountStackCanAddFrom(to.getStack(toSlot), source, to);
            if (space == 0) return 0;
            count = space;
        }
        if (toSlot < 0) {
            recurse = true;
            int[] result = getFirstValidSlotAndSpace(source, to);
            toSlot = result[0];
            if(toSlot < 0) return 0;
            count = result[1];
        }

        count = Math.min(count, limit);
        if (count == 0) return 0;
        ItemStack destination = to.getStack(toSlot);

        if (destination == ItemStack.EMPTY) {
            ItemStack newStack = source.copy();
            newStack.setCount(count);
            to.setStack(toSlot, newStack);
        } else {
            destination.increment(count);
        }
        source.decrement(count);
        if (source.isEmpty()) from.setStack(fromSlot, ItemStack.EMPTY);

        to.markDirty();
        from.markDirty();

        if (recurse && !source.isEmpty()) return count + moveItem(from, fromSlot, to, -1, limit - count);
        return count;
    }

    private static int[] getFirstValidSlotAndSpace(ItemStack fromStack, Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            int space = amountStackCanAddFrom(stack, fromStack, inventory);
            if (space > 0) {
                return new int[]{i, space};
            }
        }
        return new int[]{-1, 0};
    }

    private static int amountStackCanAddFrom(ItemStack existingStack, ItemStack fromStack, Inventory inventory) {
        if (fromStack.isEmpty()) {
            return 0;
        }
        else if (existingStack.isEmpty()) {
            return Math.min(Math.min(existingStack.getMaxCount(),
                                     inventory.getMaxCountPerStack()),
                                     fromStack.getCount());
        }
        else if (InventoryMethods.areItemsEqual(existingStack, fromStack) &&
                  existingStack.isStackable() &&
                  existingStack.getCount() < existingStack.getMaxCount() &&
                  existingStack.getCount() < inventory.getMaxCountPerStack()) {
            int stackSpace = existingStack.getMaxCount() - existingStack.getCount();
            int invSpace = inventory.getMaxCountPerStack() - existingStack.getCount();
            return Math.min(Math.min(stackSpace, invSpace), fromStack.getCount());
        }
        return 0;
    }

    private static boolean areItemsEqual(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && ItemStack.areTagsEqual(stack1, stack2);
    }
}
