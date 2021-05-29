/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle.event;

/**
 * A basic action that a turtle may perform, as accessed by the {@code turtle} API.
 *
 * @see TurtleActionEvent
 */
public enum TurtleAction
{
    /**
     * A turtle moves to a new position.
     *
     * @see TurtleBlockEvent.Move
     */
    MOVE,

    /**
     * A turtle turns in a specific direction.
     */
    TURN,

    /**
     * A turtle attempts to dig a block.
     *
     * @see TurtleBlockEvent.Dig
     */
    DIG,

    /**
     * A turtle attempts to place a block or item in the world.
     *
     * @see TurtleBlockEvent.Place
     */
    PLACE,

    /**
     * A turtle attempts to attack an entity.
     *
     * @see TurtleActionEvent
     */
    ATTACK,

    /**
     * Drop an item into an inventory/the world.
     *
     * @see TurtleInventoryEvent.Drop
     */
    DROP,

    /**
     * Suck an item from an inventory or the world.
     *
     * @see TurtleInventoryEvent.Suck
     */
    SUCK,

    /**
     * Refuel the turtle's fuel levels.
     */
    REFUEL,

    /**
     * Equip or unequip an item.
     */
    EQUIP,

    /**
     * Inspect a block in world.
     *
     * @see TurtleBlockEvent.Inspect
     */
    INSPECT,

    /**
     * Gather metdata about an item in the turtle's inventory.
     */
    INSPECT_ITEM,
}
