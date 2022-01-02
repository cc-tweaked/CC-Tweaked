/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.apis;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.turtle.core.*;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Turtles are a robotic device, which can break and place blocks, attack mobs, and move about the world. They have
 * an internal inventory of 16 slots, allowing them to store blocks they have broken or would like to place.
 *
 * ## Movement
 * Turtles are capable of moving throug the world. As turtles are blocks themselves, they are confined to Minecraft's
 * grid, moving a single block at a time.
 *
 * {@literal @}{turtle.forward} and @{turtle.back} move the turtle in the direction it is facing, while @{turtle.up} and
 * {@literal @}{turtle.down} move it up and down (as one might expect!). In order to move left or right, you first need
 * to turn the turtle using @{turtle.turnLeft}/@{turtle.turnRight} and then move forward or backwards.
 *
 * :::info
 * The name "turtle" comes from [Turtle graphics], which originated from the Logo programming language. Here you'd move
 * a turtle with various commands like "move 10" and "turn left", much like ComputerCraft's turtles!
 * :::
 *
 * Moving a turtle (though not turning it) consumes *fuel*. If a turtle does not have any @{turtle.refuel|fuel}, it
 * won't move, and the movement functions will return @{false}. If your turtle isn't going anywhere, the first thing to
 * check is if you've fuelled your turtle.
 *
 * :::tip Handling errors
 * Many turtle functions can fail in various ways. For instance, a turtle cannot move forward if there's already a block
 * there. Instead of erroring, functions which can fail either return @{true} if they succeed, or @{false} and some
 * error message if they fail.
 *
 * Unexpected failures can often lead to strange behaviour. It's often a good idea to check the return values of these
 * functions, or wrap them in @{assert} (for instance, use `assert(turtle.forward())` rather than `turtle.forward()`),
 * so the program doesn't misbehave.
 * :::
 *
 * ## Turtle upgrades
 * While a normal turtle can move about the world and place blocks, its functionality is limited. Thankfully, turtles
 * can be upgraded with *tools* and @{peripheral|peripherals}. Turtles have two upgrade slots, one on the left and right
 * sides. Upgrades can be equipped by crafting a turtle with the upgrade, or calling the @{turtle.equipLeft}/@{turtle.equipRight}
 * functions.
 *
 * Turtle tools allow you to break blocks (@{turtle.dig}) and attack entities (@{turtle.attack}). Some tools are more
 * suitable to a task than others. For instance, a diamond pickaxe can break every block, while a sword does more
 * damage. Other tools have more niche use-cases, for instance hoes can til dirt.
 *
 * Peripherals (such as the @{modem|wireless modem} or @{speaker}) can also be equipped as upgrades. These are then
 * accessible by accessing the `"left"` or `"right"` peripheral.
 *
 * [Turtle Graphics]: https://en.wikipedia.org/wiki/Turtle_graphics "Turtle graphics"
 * @cc.module turtle
 * @cc.since 1.3
 */
public class TurtleAPI implements ILuaAPI
{
    private final IAPIEnvironment environment;
    private final ITurtleAccess turtle;

    public TurtleAPI( IAPIEnvironment environment, ITurtleAccess turtle )
    {
        this.environment = environment;
        this.turtle = turtle;
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "turtle" };
    }

    private MethodResult trackCommand( ITurtleCommand command )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return turtle.executeCommand( command );
    }

    /**
     * Move the turtle forward one block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully move.
     * @cc.treturn string|nil The reason the turtle could not move.
     */
    @LuaFunction
    public final MethodResult forward()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.FORWARD ) );
    }

    /**
     * Move the turtle backwards one block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully move.
     * @cc.treturn string|nil The reason the turtle could not move.
     */
    @LuaFunction
    public final MethodResult back()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.BACK ) );
    }

    /**
     * Move the turtle up one block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully move.
     * @cc.treturn string|nil The reason the turtle could not move.
     */
    @LuaFunction
    public final MethodResult up()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.UP ) );
    }

    /**
     * Move the turtle down one block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully move.
     * @cc.treturn string|nil The reason the turtle could not move.
     */
    @LuaFunction
    public final MethodResult down()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.DOWN ) );
    }

    /**
     * Rotate the turtle 90 degress to the left.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully turn.
     * @cc.treturn string|nil The reason the turtle could not turn.
     */
    @LuaFunction
    public final MethodResult turnLeft()
    {
        return trackCommand( new TurtleTurnCommand( TurnDirection.LEFT ) );
    }

    /**
     * Rotate the turtle 90 degress to the right.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully turn.
     * @cc.treturn string|nil The reason the turtle could not turn.
     */
    @LuaFunction
    public final MethodResult turnRight()
    {
        return trackCommand( new TurtleTurnCommand( TurnDirection.RIGHT ) );
    }

    /**
     * Attempt to break the block in front of the turtle.
     *
     * This requires a turtle tool capable of breaking the block. Diamond pickaxes
     * (mining turtles) can break any vanilla block, but other tools (such as axes)
     * are more limited.
     *
     * @param side The specific tool to use. Should be "left" or "right".
     * @return The turtle command result.
     * @cc.treturn boolean Whether a block was broken.
     * @cc.treturn string|nil The reason no block was broken.
     * @cc.changed 1.6 Added optional side argument.
     */
    @LuaFunction
    public final MethodResult dig( Optional<TurtleSide> side )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.FORWARD, side.orElse( null ) ) );
    }

    /**
     * Attempt to break the block above the turtle. See {@link #dig} for full details.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether a block was broken.
     * @cc.treturn string|nil The reason no block was broken.
     * @cc.changed 1.6 Added optional side argument.
     */
    @LuaFunction
    public final MethodResult digUp( Optional<TurtleSide> side )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.UP, side.orElse( null ) ) );
    }

    /**
     * Attempt to break the block below the turtle. See {@link #dig} for full details.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether a block was broken.
     * @cc.treturn string|nil The reason no block was broken.
     * @cc.changed 1.6 Added optional side argument.
     */
    @LuaFunction
    public final MethodResult digDown( Optional<TurtleSide> side )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.DOWN, side.orElse( null ) ) );
    }

    /**
     * Place a block or item into the world in front of the turtle.
     *
     * "Placing" an item allows it to interact with blocks and entities in front of the turtle. For instance, buckets
     * can pick up and place down fluids, and wheat can be used to breed cows. However, you cannot use {@link #place} to
     * perform arbitrary block interactions, such as clicking buttons or flipping levers.
     *
     * @param args Arguments to place.
     * @return The turtle command result.
     * @cc.tparam [opt] string text When placing a sign, set its contents to this text.
     * @cc.treturn boolean Whether the block could be placed.
     * @cc.treturn string|nil The reason the block was not placed.
     * @cc.since 1.4
     */
    @LuaFunction
    public final MethodResult place( IArguments args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.FORWARD, args.getAll() ) );
    }

    /**
     * Place a block or item into the world above the turtle.
     *
     * @param args Arguments to place.
     * @return The turtle command result.
     * @cc.tparam [opt] string text When placing a sign, set its contents to this text.
     * @cc.treturn boolean Whether the block could be placed.
     * @cc.treturn string|nil The reason the block was not placed.
     * @cc.since 1.4
     * @see #place For more information about placing items.
     */
    @LuaFunction
    public final MethodResult placeUp( IArguments args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.UP, args.getAll() ) );
    }

    /**
     * Place a block or item into the world below the turtle.
     *
     * @param args Arguments to place.
     * @return The turtle command result.
     * @cc.tparam [opt] string text When placing a sign, set its contents to this text.
     * @cc.treturn boolean Whether the block could be placed.
     * @cc.treturn string|nil The reason the block was not placed.
     * @cc.since 1.4
     * @see #place For more information about placing items.
     */
    @LuaFunction
    public final MethodResult placeDown( IArguments args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.DOWN, args.getAll() ) );
    }

    /**
     * Drop the currently selected stack into the inventory in front of the turtle, or as an item into the world if
     * there is no inventory.
     *
     * @param count The number of items to drop. If not given, the entire stack will be dropped.
     * @return The turtle command result.
     * @throws LuaException If dropping an invalid number of items.
     * @cc.treturn boolean Whether items were dropped.
     * @cc.treturn string|nil The reason the no items were dropped.
     * @cc.since 1.31
     * @see #select
     */
    @LuaFunction
    public final MethodResult drop( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleDropCommand( InteractDirection.FORWARD, checkCount( count ) ) );
    }

    /**
     * Drop the currently selected stack into the inventory above the turtle, or as an item into the world if there is
     * no inventory.
     *
     * @param count The number of items to drop. If not given, the entire stack will be dropped.
     * @return The turtle command result.
     * @throws LuaException If dropping an invalid number of items.
     * @cc.treturn boolean Whether items were dropped.
     * @cc.treturn string|nil The reason the no items were dropped.
     * @cc.since 1.4
     * @see #select
     */
    @LuaFunction
    public final MethodResult dropUp( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleDropCommand( InteractDirection.UP, checkCount( count ) ) );
    }

    /**
     * Drop the currently selected stack into the inventory in front of the turtle, or as an item into the world if
     * there is no inventory.
     *
     * @param count The number of items to drop. If not given, the entire stack will be dropped.
     * @return The turtle command result.
     * @throws LuaException If dropping an invalid number of items.
     * @cc.treturn boolean Whether items were dropped.
     * @cc.treturn string|nil The reason the no items were dropped.
     * @cc.since 1.4
     * @see #select
     */
    @LuaFunction
    public final MethodResult dropDown( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleDropCommand( InteractDirection.DOWN, checkCount( count ) ) );
    }

    /**
     * Change the currently selected slot.
     *
     * The selected slot is determines what slot actions like {@link #drop} or {@link #getItemCount} act on.
     *
     * @param slot The slot to select.
     * @return The turtle command result.
     * @throws LuaException If the slot is out of range.
     * @cc.treturn true When the slot has been selected.
     * @see #getSelectedSlot
     */

    @LuaFunction
    public final MethodResult select( int slot ) throws LuaException
    {
        int actualSlot = checkSlot( slot );
        return turtle.executeCommand( turtle -> {
            turtle.setSelectedSlot( actualSlot );
            return TurtleCommandResult.success();
        } );
    }

    /**
     * Get the number of items in the given slot.
     *
     * @param slot The slot we wish to check. Defaults to the {@link #select selected slot}.
     * @return The number of items in this slot.
     * @throws LuaException If the slot is out of range.
     */
    @LuaFunction
    public final int getItemCount( Optional<Integer> slot ) throws LuaException
    {
        int actualSlot = checkSlot( slot ).orElse( turtle.getSelectedSlot() );
        return turtle.getInventory().getItem( actualSlot ).getCount();
    }

    /**
     * Get the remaining number of items which may be stored in this stack.
     *
     * For instance, if a slot contains 13 blocks of dirt, it has room for another 51.
     *
     * @param slot The slot we wish to check. Defaults to the {@link #select selected slot}.
     * @return The space left in in this slot.
     * @throws LuaException If the slot is out of range.
     */
    @LuaFunction
    public final int getItemSpace( Optional<Integer> slot ) throws LuaException
    {
        int actualSlot = checkSlot( slot ).orElse( turtle.getSelectedSlot() );
        ItemStack stack = turtle.getInventory().getItem( actualSlot );
        return stack.isEmpty() ? 64 : Math.min( stack.getMaxStackSize(), 64 ) - stack.getCount();
    }

    /**
     * Check if there is a solid block in front of the turtle. In this case, solid refers to any non-air or liquid
     * block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean If there is a solid block in front.
     */
    @LuaFunction
    public final MethodResult detect()
    {
        return trackCommand( new TurtleDetectCommand( InteractDirection.FORWARD ) );
    }

    /**
     * Check if there is a solid block above the turtle. In this case, solid refers to any non-air or liquid block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean If there is a solid block in front.
     */
    @LuaFunction
    public final MethodResult detectUp()
    {
        return trackCommand( new TurtleDetectCommand( InteractDirection.UP ) );
    }

    /**
     * Check if there is a solid block below the turtle. In this case, solid refers to any non-air or liquid block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean If there is a solid block in front.
     */
    @LuaFunction
    public final MethodResult detectDown()
    {
        return trackCommand( new TurtleDetectCommand( InteractDirection.DOWN ) );
    }

    /**
     * Check if the block in front of the turtle is equal to the item in the currently selected slot.
     *
     * @return If the block and item are equal.
     * @cc.treturn boolean If the block and item are equal.
     * @cc.since 1.31
     */
    @LuaFunction
    public final MethodResult compare()
    {
        return trackCommand( new TurtleCompareCommand( InteractDirection.FORWARD ) );
    }

    /**
     * Check if the block above the turtle is equal to the item in the currently selected slot.
     *
     * @return If the block and item are equal.
     * @cc.treturn boolean If the block and item are equal.
     * @cc.since 1.31
     */
    @LuaFunction
    public final MethodResult compareUp()
    {
        return trackCommand( new TurtleCompareCommand( InteractDirection.UP ) );
    }

    /**
     * Check if the block below the turtle is equal to the item in the currently selected slot.
     *
     * @return If the block and item are equal.
     * @cc.treturn boolean If the block and item are equal.
     * @cc.since 1.31
     */
    @LuaFunction
    public final MethodResult compareDown()
    {
        return trackCommand( new TurtleCompareCommand( InteractDirection.DOWN ) );
    }

    /**
     * Attack the entity in front of the turtle.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether an entity was attacked.
     * @cc.treturn string|nil The reason nothing was attacked.
     * @cc.since 1.4
     * @cc.changed 1.6 Added optional side argument.
     */
    @LuaFunction
    public final MethodResult attack( Optional<TurtleSide> side )
    {
        return trackCommand( TurtleToolCommand.attack( InteractDirection.FORWARD, side.orElse( null ) ) );
    }

    /**
     * Attack the entity above the turtle.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether an entity was attacked.
     * @cc.treturn string|nil The reason nothing was attacked.
     * @cc.since 1.4
     * @cc.changed 1.6 Added optional side argument.
     */
    @LuaFunction
    public final MethodResult attackUp( Optional<TurtleSide> side )
    {
        return trackCommand( TurtleToolCommand.attack( InteractDirection.UP, side.orElse( null ) ) );
    }

    /**
     * Attack the entity below the turtle.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether an entity was attacked.
     * @cc.treturn string|nil The reason nothing was attacked.
     * @cc.since 1.4
     * @cc.changed 1.6 Added optional side argument.
     */
    @LuaFunction
    public final MethodResult attackDown( Optional<TurtleSide> side )
    {
        return trackCommand( TurtleToolCommand.attack( InteractDirection.DOWN, side.orElse( null ) ) );
    }

    /**
     * Suck an item from the inventory in front of the turtle, or from an item floating in the world.
     *
     * This will pull items into the first acceptable slot, starting at the {@link #select currently selected} one.
     *
     * @param count The number of items to suck. If not given, up to a stack of items will be picked up.
     * @return The turtle command result.
     * @throws LuaException If given an invalid number of items.
     * @cc.treturn boolean Whether items were picked up.
     * @cc.treturn string|nil The reason the no items were picked up.
     * @cc.since 1.4
     * @cc.changed 1.6 Added an optional limit argument.
     */
    @LuaFunction
    public final MethodResult suck( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleSuckCommand( InteractDirection.FORWARD, checkCount( count ) ) );
    }

    /**
     * Suck an item from the inventory above the turtle, or from an item floating in the world.
     *
     * @param count The number of items to suck. If not given, up to a stack of items will be picked up.
     * @return The turtle command result.
     * @throws LuaException If given an invalid number of items.
     * @cc.treturn boolean Whether items were picked up.
     * @cc.treturn string|nil The reason the no items were picked up.
     * @cc.since 1.4
     * @cc.changed 1.6 Added an optional limit argument.
     */
    @LuaFunction
    public final MethodResult suckUp( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleSuckCommand( InteractDirection.UP, checkCount( count ) ) );
    }

    /**
     * Suck an item from the inventory below the turtle, or from an item floating in the world.
     *
     * @param count The number of items to suck. If not given, up to a stack of items will be picked up.
     * @return The turtle command result.
     * @throws LuaException If given an invalid number of items.
     * @cc.treturn boolean Whether items were picked up.
     * @cc.treturn string|nil The reason the no items were picked up.
     * @cc.since 1.4
     * @cc.changed 1.6 Added an optional limit argument.
     */
    @LuaFunction
    public final MethodResult suckDown( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleSuckCommand( InteractDirection.DOWN, checkCount( count ) ) );
    }

    /**
     * Get the maximum amount of fuel this turtle currently holds.
     *
     * @return The fuel level, or "unlimited".
     * @cc.treturn [1] number The current amount of fuel a turtle this turtle has.
     * @cc.treturn [2] "unlimited" If turtles do not consume fuel when moving.
     * @cc.since 1.4
     * @see #getFuelLimit()
     * @see #refuel(Optional)
     */
    @LuaFunction
    public final Object getFuelLevel()
    {
        return turtle.isFuelNeeded() ? turtle.getFuelLevel() : "unlimited";
    }

    /**
     * Refuel this turtle.
     *
     * While most actions a turtle can perform (such as digging or placing blocks) are free, moving consumes fuel from
     * the turtle's internal buffer. If a turtle has no fuel, it will not move.
     *
     * {@link #refuel} refuels the turtle, consuming fuel items (such as coal or lava buckets) from the currently
     * selected slot and converting them into energy. This finishes once the turtle is fully refuelled or all items have
     * been consumed.
     *
     * @param countA The maximum number of items to consume. One can pass `0` to check if an item is combustable or not.
     * @return If this turtle could be refuelled.
     * @throws LuaException If the refuel count is out of range.
     * @cc.treturn [1] true If the turtle was refuelled.
     * @cc.treturn [2] false If the turtle was not refuelled.
     * @cc.treturn [2] string The reason the turtle was not refuelled (
     * @cc.usage Refuel a turtle from the currently selected slot.
     * <pre>{@code
     * local level = turtle.getFuelLevel()
     * if new_level == "unlimited" then error("Turtle does not need fuel", 0) end
     *
     * local ok, err = turtle.refuel()
     * if ok then
     *   local new_level = turtle.getFuelLevel()
     *   print(("Refuelled %d, current level is %d"):format(new_level - level, new_level))
     * else
     *   printError(err)
     * end}</pre>
     * @cc.usage Check if the current item is a valid fuel source.
     * <pre>{@code
     * local is_fuel, reason = turtle.refuel(0)
     * if not is_fuel then printError(reason) end
     * }</pre>
     * @cc.since 1.4
     * @see #getFuelLevel()
     * @see #getFuelLimit()
     */
    @LuaFunction
    public final MethodResult refuel( Optional<Integer> countA ) throws LuaException
    {
        int count = countA.orElse( Integer.MAX_VALUE );
        if( count < 0 ) throw new LuaException( "Refuel count " + count + " out of range" );
        return trackCommand( new TurtleRefuelCommand( count ) );
    }

    /**
     * Compare the item in the currently selected slot to the item in another slot.
     *
     * @param slot The slot to compare to.
     * @return If the items are the same.
     * @throws LuaException If the slot is out of range.
     * @cc.treturn boolean If the two items are equal.
     * @cc.since 1.4
     */
    @LuaFunction
    public final MethodResult compareTo( int slot ) throws LuaException
    {
        return trackCommand( new TurtleCompareToCommand( checkSlot( slot ) ) );
    }

    /**
     * Move an item from the selected slot to another one.
     *
     * @param slotArg  The slot to move this item to.
     * @param countArg The maximum number of items to move.
     * @return If the item was moved or not.
     * @throws LuaException If the slot is out of range.
     * @throws LuaException If the number of items is out of range.
     * @cc.treturn boolean If some items were successfully moved.
     * @cc.since 1.45
     */
    @LuaFunction
    public final MethodResult transferTo( int slotArg, Optional<Integer> countArg ) throws LuaException
    {
        int slot = checkSlot( slotArg );
        int count = checkCount( countArg );
        return trackCommand( new TurtleTransferToCommand( slot, count ) );
    }

    /**
     * Get the currently selected slot.
     *
     * @return The current slot.
     * @cc.since 1.6
     * @see #select
     */
    @LuaFunction
    public final int getSelectedSlot()
    {
        return turtle.getSelectedSlot() + 1;
    }

    /**
     * Get the maximum amount of fuel this turtle can hold.
     *
     * By default, normal turtles have a limit of 20,000 and advanced turtles of 100,000.
     *
     * @return The limit, or "unlimited".
     * @cc.treturn [1] number The maximum amount of fuel a turtle can hold.
     * @cc.treturn [2] "unlimited" If turtles do not consume fuel when moving.
     * @cc.since 1.6
     * @see #getFuelLevel()
     * @see #refuel(Optional)
     */
    @LuaFunction
    public final Object getFuelLimit()
    {
        return turtle.isFuelNeeded() ? turtle.getFuelLimit() : "unlimited";
    }

    /**
     * Equip (or unequip) an item on the left side of this turtle.
     *
     * This finds the item in the currently selected slot and attempts to equip it to the left side of the turtle. The
     * previous upgrade is removed and placed into the turtle's inventory. If there is no item in the slot, the previous
     * upgrade is removed, but no new one is equipped.
     *
     * @return Whether an item was equiped or not.
     * @cc.treturn [1] true If the item was equipped.
     * @cc.treturn [2] false If we could not equip the item.
     * @cc.treturn [2] string The reason equipping this item failed.
     * @cc.since 1.6
     * @see #equipRight()
     */
    @LuaFunction
    public final MethodResult equipLeft()
    {
        return trackCommand( new TurtleEquipCommand( TurtleSide.LEFT ) );
    }

    /**
     * Equip (or unequip) an item on the right side of this turtle.
     *
     * This finds the item in the currently selected slot and attempts to equip it to the right side of the turtle. The
     * previous upgrade is removed and placed into the turtle's inventory. If there is no item in the slot, the previous
     * upgrade is removed, but no new one is equipped.
     *
     * @return Whether an item was equiped or not.
     * @cc.treturn [1] true If the item was equipped.
     * @cc.treturn [2] false If we could not equip the item.
     * @cc.treturn [2] string The reason equipping this item failed.
     * @cc.since 1.6
     * @see #equipLeft()
     */
    @LuaFunction
    public final MethodResult equipRight()
    {
        return trackCommand( new TurtleEquipCommand( TurtleSide.RIGHT ) );
    }

    /**
     * Get information about the block in front of the turtle.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether there is a block in front of the turtle.
     * @cc.treturn table|string Information about the block in front, or a message explaining that there is no block.
     * @cc.since 1.64
     * @cc.changed 1.76 Added block state to return value.
     * @cc.usage <pre>{@code
     * local has_block, data = turtle.inspect()
     * if has_block then
     *   print(textutils.serialise(data))
     *   -- {
     *   --   name = "minecraft:oak_log",
     *   --   state = { axis = "x" },
     *   --   tags = { ["minecraft:logs"] = true, ... },
     *   -- }
     * else
     *   print("No block in front of the turtle")
     * end}</pre>
     */
    @LuaFunction
    public final MethodResult inspect()
    {
        return trackCommand( new TurtleInspectCommand( InteractDirection.FORWARD ) );
    }

    /**
     * Get information about the block above the turtle.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether there is a block above the turtle.
     * @cc.treturn table|string Information about the above below, or a message explaining that there is no block.
     * @cc.since 1.64
     */
    @LuaFunction
    public final MethodResult inspectUp()
    {
        return trackCommand( new TurtleInspectCommand( InteractDirection.UP ) );
    }

    /**
     * Get information about the block below the turtle.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether there is a block below the turtle.
     * @cc.treturn table|string Information about the block below, or a message explaining that there is no block.
     * @cc.since 1.64
     */
    @LuaFunction
    public final MethodResult inspectDown()
    {
        return trackCommand( new TurtleInspectCommand( InteractDirection.DOWN ) );
    }

    /**
     * Get detailed information about the items in the given slot.
     *
     * @param context  The Lua context
     * @param slot     The slot to get information about. Defaults to the {@link #select selected slot}.
     * @param detailed Whether to include "detailed" information. When {@code true} the method will contain much
     *                 more information about the item at the cost of taking longer to run.
     * @return The command result.
     * @throws LuaException If the slot is out of range.
     * @cc.treturn nil|table Information about the given slot, or {@code nil} if it is empty.
     * @cc.since 1.64
     * @cc.usage Print the current slot, assuming it contains 13 dirt.
     *
     * <pre>{@code
     * print(textutils.serialise(turtle.getItemDetail()))
     * -- => {
     * --  name = "minecraft:dirt",
     * --  count = 13,
     * -- }
     * }</pre>
     * @see InventoryMethods#getItemDetail Describes the information returned by a detailed query.
     */
    @LuaFunction
    public final MethodResult getItemDetail( ILuaContext context, Optional<Integer> slot, Optional<Boolean> detailed ) throws LuaException
    {
        int actualSlot = checkSlot( slot ).orElse( turtle.getSelectedSlot() );
        return detailed.orElse( false )
            ? context.executeMainThreadTask( () -> getItemDetail( actualSlot, true ) )
            : MethodResult.of( getItemDetail( actualSlot, false ) );
    }

    private Object[] getItemDetail( int slot, boolean detailed )
    {
        ItemStack stack = turtle.getInventory().getItem( slot );
        if( stack.isEmpty() ) return new Object[] { null };

        Map<String, Object> table = detailed
            ? ItemData.fill( new HashMap<>(), stack )
            : ItemData.fillBasicSafe( new HashMap<>(), stack );

        return new Object[] { table };
    }


    private static int checkSlot( int slot ) throws LuaException
    {
        if( slot < 1 || slot > 16 ) throw new LuaException( "Slot number " + slot + " out of range" );
        return slot - 1;
    }

    private static Optional<Integer> checkSlot( Optional<Integer> slot ) throws LuaException
    {
        return slot.isPresent() ? Optional.of( checkSlot( slot.get() ) ) : Optional.empty();
    }

    private static int checkCount( Optional<Integer> countArg ) throws LuaException
    {
        int count = countArg.orElse( 64 );
        if( count < 0 || count > 64 ) throw new LuaException( "Item count " + count + " out of range" );
        return count;
    }
}
