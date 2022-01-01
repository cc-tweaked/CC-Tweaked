/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.ComputerCraftAPI;

import javax.annotation.Nonnull;

/**
 * An object which may be part of a wired network.
 *
 * Elements should construct a node using {@link ComputerCraftAPI#createWiredNodeForElement(IWiredElement)}. This acts
 * as a proxy for all network objects. Whilst the node may change networks, an element's node should remain constant
 * for its lifespan.
 *
 * Elements are generally tied to a block or tile entity in world. In such as case, one should provide the
 * {@link IWiredElement} capability for the appropriate sides.
 */
public interface IWiredElement extends IWiredSender
{
    /**
     * Called when objects on the network change. This may occur when network nodes are added or removed, or when
     * peripherals change.
     *
     * @param change The change which occurred.
     * @see IWiredNetworkChange
     */
    default void networkChanged( @Nonnull IWiredNetworkChange change )
    {
    }
}
