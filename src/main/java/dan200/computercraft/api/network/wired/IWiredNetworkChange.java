/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Represents a change to the objects on a wired network.
 *
 * @see IWiredElement#networkChanged(IWiredNetworkChange)
 */
public interface IWiredNetworkChange
{
    /**
     * A set of peripherals which have been removed. Note that there may be entries with the same name
     * in the added and removed set, but with a different peripheral.
     *
     * @return The set of removed peripherals.
     */
    @Nonnull
    Map<String, IPeripheral> peripheralsRemoved();

    /**
     * A set of peripherals which have been added. Note that there may be entries with the same name
     * in the added and removed set, but with a different peripheral.
     *
     * @return The set of added peripherals.
     */
    @Nonnull
    Map<String, IPeripheral> peripheralsAdded();
}
