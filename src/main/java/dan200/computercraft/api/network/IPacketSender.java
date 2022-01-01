/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.network;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * An object on a {@link IPacketNetwork}, capable of sending packets.
 */
public interface IPacketSender
{
    /**
     * Get the world in which this packet sender exists.
     *
     * @return The sender's world.
     */
    @Nonnull
    Level getLevel();

    /**
     * Get the position in the world at which this sender exists.
     *
     * @return The sender's position.
     */
    @Nonnull
    Vec3 getPosition();

    /**
     * Get some sort of identification string for this sender. This does not strictly need to be unique, but you
     * should be able to extract some identifiable information from it.
     *
     * @return This device's id.
     */
    @Nonnull
    String getSenderID();
}
