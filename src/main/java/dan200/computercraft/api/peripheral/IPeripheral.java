/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.lua.LuaFunction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * The interface that defines a peripheral.
 *
 * In order to expose a peripheral for your block or tile entity, you may either attach a {@link Capability}, or
 * register a {@link IPeripheralProvider}. This <em>cannot</em> be implemented {@link IPeripheral} directly on the tile.
 *
 * Peripherals should provide a series of methods to the user, either using {@link LuaFunction} or by implementing
 * {@link IDynamicPeripheral}.
 */
public interface IPeripheral
{
    /**
     * Should return a string that uniquely identifies this type of peripheral.
     * This can be queried from lua by calling {@code peripheral.getType()}
     *
     * @return A string identifying the type of peripheral.
     */
    @Nonnull
    String getType();

    /**
     * Return additional types/traits associated with this object.
     *
     * @return A collection of additional object traits.
     * @see PeripheralType#getAdditionalTypes()
     */
    @Nonnull
    default Set<String> getAdditionalTypes()
    {
        return Collections.emptySet();
    }

    /**
     * Is called when when a computer is attaching to the peripheral.
     *
     * This will occur when a peripheral is placed next to an active computer, when a computer is turned on next to a
     * peripheral, when a turtle travels into a square next to a peripheral, or when a wired modem adjacent to this
     * peripheral is does any of the above.
     *
     * Between calls to attach and {@link #detach}, the attached computer can make method calls on the peripheral using
     * {@code peripheral.call()}. This method can be used to keep track of which computers are attached to the
     * peripheral, or to take action when attachment occurs.
     *
     * Be aware that will be called from both the server thread and ComputerCraft Lua thread, and so must be thread-safe
     * and reentrant.
     *
     * @param computer The interface to the computer that is being attached. Remember that multiple computers can be
     *                 attached to a peripheral at once.
     * @see #detach
     */
    default void attach( @Nonnull IComputerAccess computer )
    {
    }

    /**
     * Called when a computer is detaching from the peripheral.
     *
     * This will occur when a computer shuts down, when the peripheral is removed while attached to computers, when a
     * turtle moves away from a block attached to a peripheral, or when a wired modem adjacent to this peripheral is
     * detached.
     *
     * This method can be used to keep track of which computers are attached to the peripheral, or to take action when
     * detachment occurs.
     *
     * Be aware that this will be called from both the server and ComputerCraft Lua thread, and must be thread-safe
     * and reentrant.
     *
     * @param computer The interface to the computer that is being detached. Remember that multiple computers can be
     *                 attached to a peripheral at once.
     * @see #attach
     */
    default void detach( @Nonnull IComputerAccess computer )
    {
    }

    /**
     * Get the object that this peripheral provides methods for. This will generally be the tile entity
     * or block, but may be an inventory, entity, etc...
     *
     * @return The object this peripheral targets
     */
    @Nullable
    default Object getTarget()
    {
        return null;
    }

    /**
     * Determine whether this peripheral is equivalent to another one.
     *
     * The minimal example should at least check whether they are the same object. However, you may wish to check if
     * they point to the same block or tile entity.
     *
     * @param other The peripheral to compare against. This may be {@code null}.
     * @return Whether these peripherals are equivalent.
     */
    boolean equals( @Nullable IPeripheral other );
}
