/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The interface passed to turtle by turtles, providing methods that they can call.
 *
 * This should not be implemented by your classes. Do not interact with turtles except via this interface and
 * {@link ITurtleUpgrade}.
 */
public interface ITurtleAccess
{
    /**
     * Returns the world in which the turtle resides.
     *
     * @return the world in which the turtle resides.
     */
    @Nonnull
    Level getLevel();

    /**
     * Returns a vector containing the integer co-ordinates at which the turtle resides.
     *
     * @return a vector containing the integer co-ordinates at which the turtle resides.
     */
    @Nonnull
    BlockPos getPosition();

    /**
     * Attempt to move this turtle to a new position.
     *
     * This will preserve the turtle's internal state, such as it's inventory, computer and upgrades. It should
     * be used before playing a movement animation using {@link #playAnimation(TurtleAnimation)}.
     *
     * @param world The new world to move it to
     * @param pos   The new position to move it to.
     * @return Whether the movement was successful. It may fail if the block was not loaded or the block placement
     * was cancelled.
     * @throws UnsupportedOperationException When attempting to teleport on the client side.
     */
    boolean teleportTo( @Nonnull Level world, @Nonnull BlockPos pos );

    /**
     * Returns a vector containing the floating point co-ordinates at which the turtle is rendered.
     * This will shift when the turtle is moving.
     *
     * @param f The subframe fraction.
     * @return A vector containing the floating point co-ordinates at which the turtle resides.
     * @see #getVisualYaw(float)
     */
    @Nonnull
    Vec3 getVisualPosition( float f );

    /**
     * Returns the yaw the turtle is facing when it is rendered.
     *
     * @param f The subframe fraction.
     * @return The yaw the turtle is facing.
     * @see #getVisualPosition(float)
     */
    float getVisualYaw( float f );

    /**
     * Returns the world direction the turtle is currently facing.
     *
     * @return The world direction the turtle is currently facing.
     * @see #setDirection(Direction)
     */
    @Nonnull
    Direction getDirection();

    /**
     * Set the direction the turtle is facing. Note that this will not play a rotation animation, you will also need to
     * call {@link #playAnimation(TurtleAnimation)} to do so.
     *
     * @param dir The new direction to set. This should be on either the x or z axis (so north, south, east or west).
     * @see #getDirection()
     */
    void setDirection( @Nonnull Direction dir );

    /**
     * Get the currently selected slot in the turtle's inventory.
     *
     * @return An integer representing the current slot.
     * @see #getInventory()
     * @see #setSelectedSlot(int)
     */
    int getSelectedSlot();

    /**
     * Set the currently selected slot in the turtle's inventory.
     *
     * @param slot The slot to set. This must be greater or equal to 0 and less than the inventory size. Otherwise no
     *             action will be taken.
     * @throws UnsupportedOperationException When attempting to change the slot on the client side.
     * @see #getInventory()
     * @see #getSelectedSlot()
     */
    void setSelectedSlot( int slot );

    /**
     * Set the colour of the turtle to a RGB number.
     *
     * @param colour The colour this turtle should be changed to. This should be a RGB colour between {@code 0x000000}
     *               and {@code 0xFFFFFF} or -1 to reset to the default colour.
     * @see #getColour()
     */
    void setColour( int colour );

    /**
     * Get the colour of this turtle as a RGB number.
     *
     * @return The colour this turtle is. This will be a RGB colour between {@code 0x000000} and {@code 0xFFFFFF} or
     * -1 if it has no colour.
     * @see #setColour(int)
     */
    int getColour();

    /**
     * Get the player who owns this turtle, namely whoever placed it.
     *
     * @return This turtle's owner.
     */
    @Nullable
    GameProfile getOwningPlayer();

    /**
     * Get the inventory of this turtle.
     *
     * Note: this inventory should only be accessed and modified on the server thread.
     *
     * @return This turtle's inventory
     * @see #getItemHandler()
     */
    @Nonnull
    Container getInventory();

    /**
     * Get the inventory of this turtle as an {@link IItemHandlerModifiable}.
     *
     * Note: this inventory should only be accessed and modified on the server thread.
     *
     * @return This turtle's inventory
     * @see #getInventory()
     * @see IItemHandlerModifiable
     * @see net.minecraftforge.items.CapabilityItemHandler#ITEM_HANDLER_CAPABILITY
     */
    @Nonnull
    IItemHandlerModifiable getItemHandler();

    /**
     * Determine whether this turtle will require fuel when performing actions.
     *
     * @return Whether this turtle needs fuel.
     * @see #getFuelLevel()
     * @see #setFuelLevel(int)
     */
    boolean isFuelNeeded();

    /**
     * Get the current fuel level of this turtle.
     *
     * @return The turtle's current fuel level.
     * @see #isFuelNeeded()
     * @see #setFuelLevel(int)
     */
    int getFuelLevel();

    /**
     * Set the fuel level to a new value. It is generally preferred to use {@link #consumeFuel(int)}} or {@link #addFuel(int)}
     * instead.
     *
     * @param fuel The new amount of fuel. This must be between 0 and the fuel limit.
     * @see #getFuelLevel()
     * @see #getFuelLimit()
     * @see #addFuel(int)
     * @see #consumeFuel(int)
     */
    void setFuelLevel( int fuel );

    /**
     * Get the maximum amount of fuel a turtle can hold.
     *
     * @return The turtle's fuel limit.
     */
    int getFuelLimit();

    /**
     * Removes some fuel from the turtles fuel supply. Negative numbers can be passed in to INCREASE the fuel level of the turtle.
     *
     * @param fuel The amount of fuel to consume.
     * @return Whether the turtle was able to consume the amount of fuel specified. Will return false if you supply a number
     * greater than the current fuel level of the turtle. No fuel will be consumed if {@code false} is returned.
     * @throws UnsupportedOperationException When attempting to consume fuel on the client side.
     */
    boolean consumeFuel( int fuel );

    /**
     * Increase the turtle's fuel level by the given amount.
     *
     * @param fuel The amount to refuel with.
     * @throws UnsupportedOperationException When attempting to refuel on the client side.
     */
    void addFuel( int fuel );

    /**
     * Adds a custom command to the turtles command queue. Unlike peripheral methods, these custom commands will be executed
     * on the main thread, so are guaranteed to be able to access Minecraft objects safely, and will be queued up
     * with the turtles standard movement and tool commands. An issued command will return an unique integer, which will
     * be supplied as a parameter to a "turtle_response" event issued to the turtle after the command has completed. Look at the
     * lua source code for "rom/apis/turtle" for how to build a lua wrapper around this functionality.
     *
     * @param command An object which will execute the custom command when its point in the queue is reached
     * @return The objects the command returned when executed. you should probably return these to the player
     * unchanged if called from a peripheral method.
     * @throws UnsupportedOperationException When attempting to execute a command on the client side.
     * @see ITurtleCommand
     * @see MethodResult#pullEvent(String, ILuaCallback)
     */
    @Nonnull
    MethodResult executeCommand( @Nonnull ITurtleCommand command );

    /**
     * Start playing a specific animation. This will prevent other turtle commands from executing until
     * it is finished.
     *
     * @param animation The animation to play.
     * @throws UnsupportedOperationException When attempting to execute play an animation on the client side.
     * @see TurtleAnimation
     */
    void playAnimation( @Nonnull TurtleAnimation animation );

    /**
     * Returns the turtle on the specified side of the turtle, if there is one.
     *
     * @param side The side to get the upgrade from.
     * @return The upgrade on the specified side of the turtle, if there is one.
     * @see #setUpgrade(TurtleSide, ITurtleUpgrade)
     */
    @Nullable
    ITurtleUpgrade getUpgrade( @Nonnull TurtleSide side );

    /**
     * Set the upgrade for a given side, resetting peripherals and clearing upgrade specific data.
     *
     * @param side    The side to set the upgrade on.
     * @param upgrade The upgrade to set, may be {@code null} to clear.
     * @see #getUpgrade(TurtleSide)
     */
    void setUpgrade( @Nonnull TurtleSide side, @Nullable ITurtleUpgrade upgrade );

    /**
     * Returns the peripheral created by the upgrade on the specified side of the turtle, if there is one.
     *
     * @param side The side to get the peripheral from.
     * @return The peripheral created by the upgrade on the specified side of the turtle, {@code null} if none exists.
     */
    @Nullable
    IPeripheral getPeripheral( @Nonnull TurtleSide side );

    /**
     * Get an upgrade-specific NBT compound, which can be used to store arbitrary data.
     *
     * This will be persisted across turtle restarts and chunk loads, as well as being synced to the client. You must
     * call {@link #updateUpgradeNBTData(TurtleSide)} after modifying it.
     *
     * @param side The side to get the upgrade data for.
     * @return The upgrade-specific data.
     * @see #updateUpgradeNBTData(TurtleSide)
     */
    @Nonnull
    CompoundTag getUpgradeNBTData( @Nullable TurtleSide side );

    /**
     * Mark the upgrade-specific data as dirty on a specific side. This is required for the data to be synced to the
     * client and persisted.
     *
     * @param side The side to mark dirty.
     * @see #updateUpgradeNBTData(TurtleSide)
     */
    void updateUpgradeNBTData( @Nonnull TurtleSide side );
}
